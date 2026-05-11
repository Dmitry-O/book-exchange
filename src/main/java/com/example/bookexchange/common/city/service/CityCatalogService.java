package com.example.bookexchange.common.city.service;

import com.example.bookexchange.common.city.dto.CityAutocompleteDTO;

import java.util.List;
import java.util.Locale;

public interface CityCatalogService {

    List<CityAutocompleteDTO> autocomplete(String query, int limit, Locale locale);

    String localize(String cityValue, Locale locale);
}
