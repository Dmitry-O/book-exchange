package com.example.bookexchange.common.notification;

import com.example.bookexchange.book.dto.BookCategoryDTO;
import com.example.bookexchange.common.city.service.CityCatalogService;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NotificationDisplayLocalizerTest {

    private final NotificationDisplayLocalizer localizer =
            new NotificationDisplayLocalizer(mock(CityCatalogService.class));

    @Test
    void shouldLocalizeEveryBookCategoryWithoutBrokenEncoding_whenLocaleIsRussian() {
        Map<BookCategoryDTO, String> expectedLabels = Map.ofEntries(
                entry(BookCategoryDTO.ACTION_ADVENTURE, "Приключения и экшен"),
                entry(BookCategoryDTO.ART_DESIGN, "Искусство и дизайн"),
                entry(BookCategoryDTO.AUTOBIOGRAPHY, "Автобиография"),
                entry(BookCategoryDTO.BIOGRAPHY, "Биография"),
                entry(BookCategoryDTO.BUSINESS, "Бизнес"),
                entry(BookCategoryDTO.CHILDREN, "Детская литература"),
                entry(BookCategoryDTO.CLASSIC, "Классика"),
                entry(BookCategoryDTO.COMICS, "Комиксы"),
                entry(BookCategoryDTO.CONTEMPORARY, "Современная литература"),
                entry(BookCategoryDTO.COOKING, "Кулинария"),
                entry(BookCategoryDTO.CRIME, "Криминал"),
                entry(BookCategoryDTO.DRAMA, "Драма"),
                entry(BookCategoryDTO.EDUCATION, "Образование"),
                entry(BookCategoryDTO.FANTASY, "Фэнтези"),
                entry(BookCategoryDTO.GRAPHIC_NOVEL, "Графический роман"),
                entry(BookCategoryDTO.HEALTH, "Здоровье"),
                entry(BookCategoryDTO.HISTORY, "История"),
                entry(BookCategoryDTO.MANGA, "Манга"),
                entry(BookCategoryDTO.MEMOIR, "Мемуары"),
                entry(BookCategoryDTO.MYSTERY, "Детектив"),
                entry(BookCategoryDTO.NON_FICTION, "Нон-фикшн"),
                entry(BookCategoryDTO.NOVEL, "Роман"),
                entry(BookCategoryDTO.PHILOSOPHY, "Философия"),
                entry(BookCategoryDTO.POETRY, "Поэзия"),
                entry(BookCategoryDTO.PSYCHOLOGY, "Психология"),
                entry(BookCategoryDTO.RELIGION, "Религия"),
                entry(BookCategoryDTO.ROMANCE, "Романтика"),
                entry(BookCategoryDTO.SCIENCE, "Наука"),
                entry(BookCategoryDTO.SCIENCE_FICTION, "Научная фантастика"),
                entry(BookCategoryDTO.SELF_HELP, "Саморазвитие"),
                entry(BookCategoryDTO.TECHNOLOGY, "Технологии"),
                entry(BookCategoryDTO.THRILLER, "Триллер"),
                entry(BookCategoryDTO.TRAVEL, "Путешествия"),
                entry(BookCategoryDTO.YOUNG_ADULT, "Подростковая литература"),
                entry(BookCategoryDTO.OTHER, "Другое")
        );

        assertThat(expectedLabels).hasSize(BookCategoryDTO.values().length);

        expectedLabels.forEach((category, expectedLabel) ->
                assertThat(localizer.localizeBookCategory(category.getProperty(), Locale.forLanguageTag("ru")))
                        .as(category.name())
                        .isEqualTo(expectedLabel)
        );
    }

    @Test
    void shouldUseLocalizedOtherLabel_whenStoredCategoryIsUnknown() {
        assertThat(localizer.localizeBookCategory("unknown-category", Locale.forLanguageTag("ru")))
                .isEqualTo("Другое");
    }
}
