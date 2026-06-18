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
            entry(BookCategoryDTO.ACTION_ADVENTURE, "Action & Adventure", "Action & Abenteuer", "Приключения и экшен"),
            entry(BookCategoryDTO.ART_DESIGN, "Art & Design", "Kunst & Design", "Искусство и дизайн"),
            entry(BookCategoryDTO.AUTOBIOGRAPHY, "Autobiography", "Autobiografie", "Автобиография"),
            entry(BookCategoryDTO.BIOGRAPHY, "Biography", "Biografie", "Биография"),
            entry(BookCategoryDTO.BUSINESS, "Business", "Wirtschaft", "Бизнес"),
            entry(BookCategoryDTO.CHILDREN, "Children", "Kinder", "Детская литература"),
            entry(BookCategoryDTO.CLASSIC, "Classic", "Klassiker", "Классика"),
            entry(BookCategoryDTO.COMICS, "Comics", "Comics", "Комиксы"),
            entry(BookCategoryDTO.CONTEMPORARY, "Contemporary", "Gegenwartsliteratur", "Современная литература"),
            entry(BookCategoryDTO.COOKING, "Cooking", "Kochen", "Кулинария"),
            entry(BookCategoryDTO.CRIME, "Crime", "Krimi", "Криминал"),
            entry(BookCategoryDTO.DRAMA, "Drama", "Drama", "Драма"),
            entry(BookCategoryDTO.EDUCATION, "Education", "Bildung", "Образование"),
            entry(BookCategoryDTO.FANTASY, "Fantasy", "Fantasy", "Фэнтези"),
            entry(BookCategoryDTO.GRAPHIC_NOVEL, "Graphic Novel", "Graphic Novel", "Графический роман"),
            entry(BookCategoryDTO.HEALTH, "Health", "Gesundheit", "Здоровье"),
            entry(BookCategoryDTO.HISTORY, "History", "Geschichte", "История"),
            entry(BookCategoryDTO.MANGA, "Manga", "Manga", "Манга"),
            entry(BookCategoryDTO.MEMOIR, "Memoir", "Memoiren", "Мемуары"),
            entry(BookCategoryDTO.MYSTERY, "Mystery", "Mystery", "Детектив"),
            entry(BookCategoryDTO.NON_FICTION, "Non-fiction", "Sachbuch", "Нон-фикшн"),
            entry(BookCategoryDTO.NOVEL, "Novel", "Roman", "Роман"),
            entry(BookCategoryDTO.PHILOSOPHY, "Philosophy", "Philosophie", "Философия"),
            entry(BookCategoryDTO.POETRY, "Poetry", "Poesie", "Поэзия"),
            entry(BookCategoryDTO.PSYCHOLOGY, "Psychology", "Psychologie", "Психология"),
            entry(BookCategoryDTO.RELIGION, "Religion", "Religion", "Религия"),
            entry(BookCategoryDTO.ROMANCE, "Romance", "Romantik", "Романтика"),
            entry(BookCategoryDTO.SCIENCE, "Science", "Wissenschaft", "Наука"),
            entry(BookCategoryDTO.SCIENCE_FICTION, "Science Fiction", "Science-Fiction", "Научная фантастика"),
            entry(BookCategoryDTO.SELF_HELP, "Self-help", "Selbsthilfe", "Саморазвитие"),
            entry(BookCategoryDTO.TECHNOLOGY, "Technology", "Technologie", "Технологии"),
            entry(BookCategoryDTO.THRILLER, "Thriller", "Thriller", "Триллер"),
            entry(BookCategoryDTO.TRAVEL, "Travel", "Reisen", "Путешествия"),
            entry(BookCategoryDTO.YOUNG_ADULT, "Young Adult", "Jugendbuch", "Подростковая литература"),
            entry(BookCategoryDTO.OTHER, "Other", "Andere", "Другое")
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
