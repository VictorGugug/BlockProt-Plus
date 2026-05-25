/*
 * Copyright (C) 2021 - 2025 spnda
 * This file is part of BlockProt <https://github.com/spnda/BlockProt>.
 *
 * BlockProt is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.bukkit;

import com.google.common.collect.Sets;
import de.sean.blockprot.nbt.LockReturnValue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * Helper to quickly obtain translations from a config by an enum key.
 *
 * <h3>Color and formatting support</h3>
 * <p>Translation values may use either the legacy {@code &} color code format
 * (e.g. {@code &6}, {@code &a}, {@code &l}) or the Adventure
 * <a href="https://docs.advntr.dev/minimessage/format.html">MiniMessage</a>
 * format (e.g. {@code <gold>}, {@code <bold>}, {@code <gradient:red:blue>}).
 * Both formats are auto-detected and processed correctly.
 *
 * <p>Detection logic: if the raw value contains {@code <} followed by a
 * known MiniMessage tag name (closing or opening), it is parsed as MiniMessage
 * and then serialized back to a legacy section-symbol string for compatibility
 * with the existing Bukkit component APIs used throughout the plugin.
 * Otherwise, the legacy {@code &} translator is applied as before.
 *
 * <h3>BPR self-repair</h3>
 * <p>When a key is missing from the active language file but exists in the
 * bundled English file, the English value is used silently. Console output is
 * limited to one localized summary line; full details go to the session log.</p>
 *
 * @since 0.1.10
 */
public final class Translator {
    /**
     * The list of all included translation files.
     */
    public static final HashSet<String> DEFAULT_TRANSLATION_FILES = Sets.newHashSet(
        "translations_cs.yml", "translations_de.yml", "translations_en.yml",
        "translations_es.yml", "translations_fr.yml", "translations_it.yml",
        "translations_ja.yml", "translations_ko.yml", "translations_pl.yml",
        "translations_pt-br.yml", "translations_ru.yml", "translations_sk.yml",
        "translations_tr.yml", "translations_zh-CN.yml", "translations_zh-TW.yml"
    );

    /**
     * The default locale we use for default translation values.
     *
     * @since 0.4.6
     */
    @NotNull
    public static final Locale defaultLocale = Locale.UK;

    /**
     * A HashMap of all possible translation values by key, loaded through
     * {@link Translator#loadFromConfigs(YamlConfiguration, YamlConfiguration)}.
     *
     * @since 0.4.6
     */
    @NotNull
    private static final HashMap<TranslationKey, TranslationValue> values = new HashMap<>();

    /**
     * Represents the locale of the translated values. Defaults to {@link #defaultLocale}.
     *
     * @since 0.4.6
     */
    @NotNull
    private static Locale locale = defaultLocale;

    static String DEFAULT_FALLBACK = "";

    /** Shared MiniMessage instance — stateless, safe to reuse. */
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Converts Adventure components back to legacy section-symbol strings
     * for compatibility with Bukkit APIs used throughout the plugin.
     */
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
        LegacyComponentSerializer.legacySection();

    /**
     * @since 0.2.3
     */
    private Translator() {
    }

    /**
     * Initialize the translations from given configuration and sets the internal locale.
     *
     * <p><b>BPR self-repair:</b> keys missing from {@code config} but present
     * in {@code defaultConfig} are filled from English. Only one summary is
     * printed to console; details are written to the session log.</p>
     *
     * @param defaultConfig The English reference configuration.
     * @param config        The active language configuration.
     * @since 0.4.6
     */
    public static void loadFromConfigs(@NotNull final YamlConfiguration defaultConfig,
                                       @NotNull final YamlConfiguration config) {
        String localeStr = config.getString("locale");
        // Locale.forLanguageTag() replaces the deprecated new Locale(String) constructor.
        // Translation files use underscores (e.g. "zh_CN"); IETF tags use hyphens ("zh-CN").
        Translator.locale = (localeStr == null || localeStr.isBlank())
            ? Locale.ROOT
            : Locale.forLanguageTag(localeStr.replace('_', '-'));

        final String langFile = BlockProt.getDefaultConfig().getLanguageFile();
        TranslationKey[] translations = TranslationKey.values();

        List<String> missingInBoth   = new ArrayList<>();
        List<String> missingInActive = new ArrayList<>();

        for (TranslationKey translation : translations) {
            String translationKey = translation.toString();

            if (!defaultConfig.contains(translationKey, true)
                    && !config.contains(translationKey, true)) {
                values.put(translation, new TranslationValue(translationKey));
                missingInBoth.add(translationKey);
                continue;
            }

            Object defaultValue = defaultConfig.get(translationKey);
            TranslationValue translatedValue =
                (defaultValue instanceof String)
                    ? new TranslationValue((String) defaultValue)
                    : TranslationValue.UNKNOWN_TRANSLATION_VALUE;

            Object activeValue = config.get(translationKey);
            if (activeValue instanceof String) {
                translatedValue.setTranslatedValue((String) activeValue);
            } else {
                missingInActive.add(translationKey);
            }

            values.put(translation, translatedValue);
        }

        if (!missingInBoth.isEmpty()) {
            BlockProtLogger.log("Translator",
                "Keys missing in EN + " + langFile + " (" + missingInBoth.size() + "): "
                + String.join(", ", missingInBoth));
        }
        if (!missingInActive.isEmpty()) {
            BlockProtLogger.log("Translator",
                "English fallback keys used in " + langFile
                + " (" + missingInActive.size() + "): "
                + String.join(", ", missingInActive));
        }

        int total = missingInBoth.size() + missingInActive.size();
        if (total > 0) {
            BlockProt.getInstance().getLogger().info(
                Translator.get(TranslationKey.CONSOLE__TRANSLATIONS_INCOMPLETE)
                    .replace("{total}", String.valueOf(total))
                    .replace("{file}", langFile)
                    .replace("{fallback}", String.valueOf(missingInActive.size()))
                    .replace("{missing}", String.valueOf(missingInBoth.size())));
        }
    }

    /**
     * Returns true if the string contains a MiniMessage opening or closing tag.
     *
     * <p>Conservative check: looks for {@code <letter} (opening) or {@code </} (closing).
     * Avoids false positives on plain {@code <} characters used in math or HTML-like text.</p>
     */
    private static boolean containsMiniMessage(@NotNull String text) {
        int idx = text.indexOf('<');
        while (idx != -1) {
            if (idx + 1 < text.length()) {
                char next = text.charAt(idx + 1);
                if (Character.isLetter(next) || next == '/') return true;
            }
            idx = text.indexOf('<', idx + 1);
        }
        return false;
    }

    /**
     * Processes a raw translation string into a legacy section-symbol formatted string.
     *
     * <p>If MiniMessage tags are detected the string is parsed by Adventure and serialized
     * back to legacy format. Otherwise, legacy {@code &} color codes are translated as before.</p>
     */
    @NotNull
    private static String process(@NotNull String raw) {
        if (containsMiniMessage(raw)) {
            Component component = MINI_MESSAGE.deserialize(raw);
            return LEGACY_SERIALIZER.serialize(component);
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    /**
     * Get the translated String by translation key. This will use
     * {@link TranslationValue#getValue()}, so values that are not
     * translated still use their default value.
     *
     * <p>Supports both legacy color codes ({@code &6}, {@code &a}, {@code &l})
     * and the Adventure MiniMessage format ({@code <gold>}, {@code <bold>},
     * {@code <gradient:red:blue>}). Both formats are detected automatically.</p>
     *
     * @param key the translation key to search for.
     * @return A translated String or an empty string if not found.
     * @since 0.1.10
     */
    @NotNull
    public static String get(@NotNull final TranslationKey key) {
        TranslationValue value = values.get(key);
        String raw = value == null ? key.toString() : value.getValue();
        return process(raw);
    }

    /**
     * Gets the appropriate translation for given {@link LockReturnValue.Reason}.
     *
     * @return The translated string.
     * @since 1.0.3
     */
    @NotNull
    public static String get(@NotNull final LockReturnValue.Reason reason) {
        return switch (reason) {
            case NO_PERMISSION -> get(TranslationKey.MESSAGES__NO_PERMISSION);
            case EXCEEDED_MAX_BLOCK_COUNT -> get(TranslationKey.MESSAGES__EXCEEDED_MAX_BLOCK_COUNT);
        };
    }

    /**
     * Get the currently used locale for the current translator.
     *
     * @return The locale of translations.
     * @since 0.4.6
     */
    @NotNull
    public static Locale getLocale() {
        return locale;
    }

    /**
     * Clears all translations.
     *
     * @since 1.0.0
     */
    public static void resetTranslations() {
        values.clear();
    }
}
