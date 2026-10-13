/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.rest.service;

import org.junit.jupiter.api.Test;
import smoketest.data.rest.domain.City;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CityRepository}.
 *
 * @author Oliver Gierke
 * @author Andy Wilkinson
 */
@SpringBootTest
class CityRepositoryIntegrationTests {

	@Autowired
	CityRepository repository;

	@Test
	void findsFirstPageOfCities() {
		Page<City> cities = this.repository.findAll(PageRequest.of(0, 10));
		assertThat(cities.getTotalElements()).isGreaterThan(20L);
	}

	@Test
	void findByNameAndCountry() {
		City city = this.repository.findByNameAndCountryAllIgnoringCase("Melbourne", "Australia");
		assertThat(city).isNotNull();
		assertThat(city.getName()).isEqualTo("Melbourne");
	}

	@Test
	void findContaining() {
		Page<City> cities = this.repository.findByNameContainingAndCountryContainingAllIgnoringCase("", "UK",
				PageRequest.of(0, 10));
		assertThat(cities.getTotalElements()).isEqualTo(3L);
	}

}
