package com.example.bookexchange.common.city.service;

import com.example.bookexchange.common.city.dto.CityAutocompleteDTO;
import com.example.bookexchange.common.city.model.CityDictionaryEntry;
import com.example.bookexchange.common.city.repository.CityDictionaryRepository;
import com.example.bookexchange.common.city.util.CityNormalizationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CityCatalogServiceImpl implements CityCatalogService {

    private final CityDictionaryRepository cityDictionaryRepository;

    @Transactional(readOnly = true)
    @Override
    public List<CityAutocompleteDTO> autocomplete(String query, int limit, Locale locale) {
        String normalizedQuery = CityNormalizationUtil.normalize(query);

        return cityDictionaryRepository.searchAutocomplete(
                        normalizedQuery,
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(city -> CityAutocompleteDTO.builder()
                        .value(city.getCanonicalName())
                        .label(resolveLabel(city, locale))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public String localize(String cityValue, Locale locale) {
        String normalizedValue = CityNormalizationUtil.normalize(cityValue);

        return cityDictionaryRepository.findExactMatches(normalizedValue, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(city -> resolveLabel(city, locale))
                .orElse(cityValue);
    }

    private String resolveLabel(CityDictionaryEntry city, Locale locale) {
        String language = locale == null ? Locale.ENGLISH.getLanguage() : locale.getLanguage();

        if (Locale.GERMAN.getLanguage().equals(language)) {
            return city.getNameDe();
        }

        if ("ru".equals(language)) {
            return city.getNameRu();
        }

        return city.getNameEn();
    }
}
