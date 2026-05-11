package com.example.bookexchange.common.city.repository;

import com.example.bookexchange.common.city.model.CityDictionaryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CityDictionaryRepository extends JpaRepository<CityDictionaryEntry, Long> {

    @Query("""
            SELECT c
            FROM CityDictionaryEntry c
            WHERE c.normalizedCanonicalName = :normalizedValue
                OR c.normalizedNameEn = :normalizedValue
                OR c.normalizedNameDe = :normalizedValue
                OR c.normalizedNameRu = :normalizedValue
            ORDER BY CASE
                WHEN c.normalizedCanonicalName = :normalizedValue THEN 0
                WHEN c.normalizedNameEn = :normalizedValue THEN 1
                WHEN c.normalizedNameDe = :normalizedValue THEN 2
                ELSE 3
            END,
            c.canonicalName ASC
            """)
    Page<CityDictionaryEntry> findExactMatches(
            @Param("normalizedValue") String normalizedValue,
            Pageable pageable
    );

    @Query("""
            SELECT c
            FROM CityDictionaryEntry c
            WHERE c.normalizedCanonicalName LIKE CONCAT('%', :normalizedQuery, '%')
                OR c.normalizedNameEn LIKE CONCAT('%', :normalizedQuery, '%')
                OR c.normalizedNameDe LIKE CONCAT('%', :normalizedQuery, '%')
                OR c.normalizedNameRu LIKE CONCAT('%', :normalizedQuery, '%')
            ORDER BY CASE
                WHEN c.normalizedCanonicalName LIKE CONCAT(:normalizedQuery, '%') THEN 0
                WHEN c.normalizedNameEn LIKE CONCAT(:normalizedQuery, '%') THEN 1
                WHEN c.normalizedNameDe LIKE CONCAT(:normalizedQuery, '%') THEN 2
                WHEN c.normalizedNameRu LIKE CONCAT(:normalizedQuery, '%') THEN 3
                ELSE 4
            END,
            c.canonicalName ASC
            """)
    Page<CityDictionaryEntry> searchAutocomplete(
            @Param("normalizedQuery") String normalizedQuery,
            Pageable pageable
    );
}
