/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.jpa.service;

import org.jspecify.annotations.Nullable;
import smoketest.data.jpa.domain.City;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface CityRepository extends Repository<City, Long> {

	Page<City> findAll(@Nullable Pageable pageable);

	Page<City> findByNameContainingAndCountryContainingAllIgnoringCase(String name, String country, Pageable pageable);

	City findByNameAndCountryAllIgnoringCase(String name, String country);

}
