package com.example.bookexchange.common.notification;

import com.example.bookexchange.book.dto.BookCategoryDTO;
import com.example.bookexchange.common.city.service.CityCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
class NotificationDisplayLocalizer {

    private static final String LANG_DE = "de";
    private static final String LANG_RU = "ru";

    private static final Map<BookCategoryDTO, LocalizedValue> CATEGORY_LABELS = Map.ofEntries(
            entry(BookCategoryDTO.ACTION_ADVENTURE, "Action & Adventure", "Action & Abenteuer", "РџСЂРёРєР»СЋС‡РµРЅРёСЏ Рё СЌРєС€РµРЅ"),
            entry(BookCategoryDTO.ART_DESIGN, "Art & Design", "Kunst & Design", "РСЃРєСѓСЃСЃС‚РІРѕ Рё РґРёР·Р°Р№РЅ"),
            entry(BookCategoryDTO.AUTOBIOGRAPHY, "Autobiography", "Autobiografie", "РђРІС‚РѕР±РёРѕРіСЂР°С„РёСЏ"),
            entry(BookCategoryDTO.BIOGRAPHY, "Biography", "Biografie", "Р‘РёРѕРіСЂР°С„РёСЏ"),
            entry(BookCategoryDTO.BUSINESS, "Business", "Wirtschaft", "Р‘РёР·РЅРµСЃ"),
            entry(BookCategoryDTO.CHILDREN, "Children", "Kinder", "Р”РµС‚СЃРєР°СЏ Р»РёС‚РµСЂР°С‚СѓСЂР°"),
            entry(BookCategoryDTO.CLASSIC, "Classic", "Klassiker", "РљР»Р°СЃСЃРёРєР°"),
            entry(BookCategoryDTO.COMICS, "Comics", "Comics", "РљРѕРјРёРєСЃС‹"),
            entry(BookCategoryDTO.CONTEMPORARY, "Contemporary", "Gegenwartsliteratur", "РЎРѕРІСЂРµРјРµРЅРЅР°СЏ Р»РёС‚РµСЂР°С‚СѓСЂР°"),
            entry(BookCategoryDTO.COOKING, "Cooking", "Kochen", "РљСѓР»РёРЅР°СЂРёСЏ"),
            entry(BookCategoryDTO.CRIME, "Crime", "Krimi", "РљСЂРёРјРёРЅР°Р»"),
            entry(BookCategoryDTO.DRAMA, "Drama", "Drama", "Р”СЂР°РјР°"),
            entry(BookCategoryDTO.EDUCATION, "Education", "Bildung", "РћР±СЂР°Р·РѕРІР°РЅРёРµ"),
            entry(BookCategoryDTO.FANTASY, "Fantasy", "Fantasy", "Р¤СЌРЅС‚РµР·Рё"),
            entry(BookCategoryDTO.GRAPHIC_NOVEL, "Graphic Novel", "Graphic Novel", "Р“СЂР°С„РёС‡РµСЃРєРёР№ СЂРѕРјР°РЅ"),
            entry(BookCategoryDTO.HEALTH, "Health", "Gesundheit", "Р—РґРѕСЂРѕРІСЊРµ"),
            entry(BookCategoryDTO.HISTORY, "History", "Geschichte", "РСЃС‚РѕСЂРёСЏ"),
            entry(BookCategoryDTO.MANGA, "Manga", "Manga", "РњР°РЅРіР°"),
            entry(BookCategoryDTO.MEMOIR, "Memoir", "Memoiren", "РњРµРјСѓР°СЂС‹"),
            entry(BookCategoryDTO.MYSTERY, "Mystery", "Mystery", "Р”РµС‚РµРєС‚РёРІ"),
            entry(BookCategoryDTO.NON_FICTION, "Non-fiction", "Sachbuch", "РќРѕРЅ-С„РёРєС€РЅ"),
            entry(BookCategoryDTO.NOVEL, "Novel", "Roman", "Р РѕРјР°РЅ"),
            entry(BookCategoryDTO.PHILOSOPHY, "Philosophy", "Philosophie", "Р¤РёР»РѕСЃРѕС„РёСЏ"),
            entry(BookCategoryDTO.POETRY, "Poetry", "Poesie", "РџРѕСЌР·РёСЏ"),
            entry(BookCategoryDTO.PSYCHOLOGY, "Psychology", "Psychologie", "РџСЃРёС…РѕР»РѕРіРёСЏ"),
            entry(BookCategoryDTO.RELIGION, "Religion", "Religion", "Р РµР»РёРіРёСЏ"),
            entry(BookCategoryDTO.ROMANCE, "Romance", "Romantik", "Р РѕРјР°РЅС‚РёРєР°"),
            entry(BookCategoryDTO.SCIENCE, "Science", "Wissenschaft", "РќР°СѓРєР°"),
            entry(BookCategoryDTO.SCIENCE_FICTION, "Science Fiction", "Science-Fiction", "РќР°СѓС‡РЅР°СЏ С„Р°РЅС‚Р°СЃС‚РёРєР°"),
            entry(BookCategoryDTO.SELF_HELP, "Self-help", "Selbsthilfe", "РЎР°РјРѕСЂР°Р·РІРёС‚РёРµ"),
            entry(BookCategoryDTO.TECHNOLOGY, "Technology", "Technologie", "РўРµС…РЅРѕР»РѕРіРёРё"),
            entry(BookCategoryDTO.THRILLER, "Thriller", "Thriller", "РўСЂРёР»Р»РµСЂ"),
            entry(BookCategoryDTO.TRAVEL, "Travel", "Reisen", "РџСѓС‚РµС€РµСЃС‚РІРёСЏ"),
            entry(BookCategoryDTO.YOUNG_ADULT, "Young Adult", "Jugendbuch", "РџРѕРґСЂРѕСЃС‚РєРѕРІР°СЏ Р»РёС‚РµСЂР°С‚СѓСЂР°"),
            entry(BookCategoryDTO.OTHER, "Other", "Andere", "Р”СЂСѓРіРѕРµ")
    );

    private final CityCatalogService cityCatalogService;

    String localizeBookCategory(String value, Locale locale) {
        BookCategoryDTO category = BookCategoryDTO.fromStorageValue(value);
        LocalizedValue labels = CATEGORY_LABELS.getOrDefault(category, CATEGORY_LABELS.get(BookCategoryDTO.OTHER));
        return labels.resolve(locale, value);
    }

    String localizeCity(String value, Locale locale) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return cityCatalogService.localize(value, locale);
    }

    private static Map.Entry<BookCategoryDTO, LocalizedValue> entry(
            BookCategoryDTO category,
            String en,
            String de,
            String ru
    ) {
        return Map.entry(category, new LocalizedValue(en, de, ru));
    }

    private record LocalizedValue(String en, String de, String ru) {
        private String resolve(Locale locale, String fallback) {
            String language = locale == null ? Locale.ENGLISH.getLanguage() : locale.getLanguage();

            if (LANG_DE.equals(language)) {
                return de;
            }

            if (LANG_RU.equals(language)) {
                return ru;
            }

            return en != null && !en.isBlank() ? en : fallback;
        }
    }
}
