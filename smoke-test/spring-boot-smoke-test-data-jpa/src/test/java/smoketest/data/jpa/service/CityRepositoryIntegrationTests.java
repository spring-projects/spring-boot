/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.jpa.service;

import org.junit.jupiter.api.Test;
import smoketest.data.jpa.domain.City;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CityRepository}.
 *
 * @author Oliver Gierke
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

}
