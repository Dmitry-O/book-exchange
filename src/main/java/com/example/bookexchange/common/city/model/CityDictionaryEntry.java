package com.example.bookexchange.common.city.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "city_dictionary")
@Getter
@Setter
public class CityDictionaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "canonical_name", nullable = false, unique = true, length = 100)
    private String canonicalName;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(name = "name_de", nullable = false, length = 100)
    private String nameDe;

    @Column(name = "name_ru", nullable = false, length = 100)
    private String nameRu;

    @Column(name = "normalized_canonical_name", nullable = false, unique = true, length = 120)
    private String normalizedCanonicalName;

    @Column(name = "normalized_name_en", nullable = false, length = 120)
    private String normalizedNameEn;

    @Column(name = "normalized_name_de", nullable = false, length = 120)
    private String normalizedNameDe;

    @Column(name = "normalized_name_ru", nullable = false, length = 120)
    private String normalizedNameRu;
}
