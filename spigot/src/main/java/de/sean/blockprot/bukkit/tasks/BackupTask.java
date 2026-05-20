package de.sean.blockprot.bukkit.tasks;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtConsole;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Creates a ZIP backup of the plugin data folder before critical operations.
 *
 * <p>Backup is ONLY created when existing data is detected (blockprot.db,
 * or any nbt data file). This prevents noise on completely fresh installations.
 * When a backup is made, the console receives a clear notice and a reminder to
 * review the new config options.</p>
 *
 * <p>Backups are saved to: {@code plugins/BlockProt/backups/YYYY-MM-DD_HH-mm.zip}</p>
 *
 * <p>The optional GitHub version check is performed <em>asynchronously</em> after the
 * ZIP has been written, so it never adds latency to the server startup sequence.</p>
 *
 * @since SP26
 */
public final class BackupTask implements Runnable {

    private static final SimpleDateFormat DATE_FMT  = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
    private static final int              MAX_BACKUPS = 10;

    /** Files whose presence signals that the server has pre-existing plugin data. */
    private static final String[] DATA_SENTINELS = {
        "data.yml",
        "blockprot.db",
        "blockprot_audit.sqlite",
        "stats.yml"
    };

    private final File    dataFolder;
    /**
     * When true, the caller explicitly requested a backup (e.g. /bp reload).
     * When false, the backup is opportunistic: only runs if prior data exists.
     */
    private final boolean forced;

    public BackupTask(@NotNull File dataFolder) {
        this(dataFolder, false);
    }

    public BackupTask(@NotNull File dataFolder, boolean forced) {
        this.dataFolder = dataFolder;
        this.forced     = forced;
    }

    @Override
    public void run() {
        boolean hasPriorData = forced || hasPriorData(dataFolder);

        if (!hasPriorData) {
            // Fresh install — no backup needed, no noise in console.
            return;
        }

        File backupDir = new File(dataFolder, "backups");
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            BlockProt.getInstance().getLogger().warning(
                Translator.get(TranslationKey.CONSOLE__BACKUP_DIR_FAILED));
            return;
        }

        pruneOldBackups(backupDir);

        String version = "unknown";
        try {
            version = BlockProt.getInstance().getDescription().getVersion();
        } catch (Exception ignored) {}

        String timestamp = DATE_FMT.format(new Date());
        String suffix = "";
        if (version != null && !version.isBlank()) {
            suffix = "_v" + version.replaceAll("\\s+", "_");
        }

        File zipFile = new File(backupDir, timestamp + suffix + ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            addDirectory(dataFolder, dataFolder.getName(), zos, backupDir);

            // Minimal metadata — no network calls here.
            StringBuilder meta = new StringBuilder();
            meta.append("plugin: BlockProt\n");
            meta.append("version: ").append(version).append("\n");
            zos.putNextEntry(new ZipEntry("release_info.txt"));
            zos.write(meta.toString().getBytes());
            zos.closeEntry();

            BlockProtLogger.log("backup", "Pre-existing data detected. Backup created at "
                + zipFile.getAbsolutePath());
            BlockProtConsole.info(
                Translator.get(TranslationKey.CONSOLE__BACKUP_CREATED)
                    .replace("{file}", zipFile.getName()));
            if (!forced) {
                BlockProtConsole.info(
                    Translator.get(TranslationKey.CONSOLE__BACKUP_REVIEW_CONFIG));
            }
        } catch (IOException e) {
            BlockProt.getInstance().getLogger().warning(
                Translator.get(TranslationKey.CONSOLE__BACKUP_FAILED)
                    .replace("{error}", e.getMessage()));
            return;
        }

        // Kick off the optional GitHub version check asynchronously — it must NOT
        // block startup. Any network failure is silently discarded.
        final String currentVersion = version;
        final File finalZip = zipFile;
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () ->
            appendVersionInfoAsync(finalZip, currentVersion));
    }

    /**
     * Queries the GitHub Releases API and appends is_latest_release info to the zip's
     * metadata file. Runs completely off the main thread. All exceptions are swallowed.
     */
    private static void appendVersionInfoAsync(@NotNull File zipFile, @NotNull String version) {
        String latestTag = "";
        boolean isLatest = false;
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/VictorGugug/BlockProt-Plus/releases/latest"))
                .header("User-Agent", "BlockProt-BackupTask")
                .header("Accept", "application/vnd.github+json")
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                int idx = body.indexOf("\"tag_name\"");
                if (idx >= 0) {
                    int colon  = body.indexOf(':', idx);
                    int quote1 = body.indexOf('"', colon + 1);
                    int quote2 = body.indexOf('"', quote1 + 1);
                    if (quote1 >= 0 && quote2 > quote1) {
                        latestTag = body.substring(quote1 + 1, quote2);
                        String cmp = latestTag.startsWith("v") || latestTag.startsWith("V")
                            ? latestTag.substring(1) : latestTag;
                        isLatest = cmp.equals(version);
                    }
                }
            }
        } catch (Throwable ignored) {
            // Best-effort — network down, rate-limited, etc.
        }

        if (!latestTag.isEmpty() && zipFile.exists()) {
            // We can't reopen a ZipOutputStream to append, so we log the info instead.
            BlockProtLogger.log("backup", "GitHub version check: latest=" + latestTag
                + ", running=" + version + ", isLatest=" + isLatest);
            if (isLatest) {
                BlockProtLogger.log("backup", "Running the latest release.");
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Returns true if any known data-sentinel file exists in the data folder,
     * meaning this is an upgrade rather than a fresh installation.
     */
    public static boolean hasPriorData(@NotNull File dataFolder) {
        for (String name : DATA_SENTINELS) {
            if (new File(dataFolder, name).exists()) return true;
        }
        File[] children = dataFolder.listFiles();
        if (children == null) return false;
        for (File child : children) {
            if (!child.isDirectory()) continue;
            String name = child.getName();
            if (name.equalsIgnoreCase("backups") || name.equalsIgnoreCase("lang")) continue;
            if (containsLegacyData(child)) return true;
        }
        return false;
    }

    private static boolean containsLegacyData(@NotNull File dir) {
        File[] children = dir.listFiles();
        if (children == null) return false;
        for (File child : children) {
            if (child.isDirectory()) {
                if (child.getName().equalsIgnoreCase("DIM-1") || child.getName().equalsIgnoreCase("DIM1")) return true;
                if (containsLegacyData(child)) return true;
                continue;
            }
            String name = child.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".yml") || name.endsWith(".db") || name.endsWith(".sqlite") || name.endsWith(".dat")) {
                return true;
            }
        }
        return false;
    }

    private void addDirectory(@NotNull File dir, @NotNull String base,
                              @NotNull ZipOutputStream zos, @NotNull File backupDir)
            throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            // Never recurse into the backups folder itself, and skip session logs.
            if (file.equals(backupDir) || file.getName().endsWith(".log")) continue;

            String entry = base + "/" + file.getName();
            if (file.isDirectory()) {
                addDirectory(file, entry, zos, backupDir);
            } else {
                zos.putNextEntry(new ZipEntry(entry));
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.transferTo(zos);
                }
                zos.closeEntry();
            }
        }
    }

    private void pruneOldBackups(@NotNull File backupDir) {
        File[] zips = backupDir.listFiles((d, name) -> name.endsWith(".zip"));
        if (zips == null || zips.length < MAX_BACKUPS) return;

        Arrays.sort(zips, Comparator.comparingLong(File::lastModified));
        for (int i = 0; i <= zips.length - MAX_BACKUPS; i++) {
            //noinspection ResultOfMethodCallIgnored
            zips[i].delete();
        }
    }
}
