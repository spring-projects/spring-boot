/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.data.rest.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import sample.data.rest.domain.City;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CityRepository}.
 *
 * @author Oliver Gierke
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class CityRepositoryIntegrationTests {

	@Autowired
	CityRepository repository;

	@Test
	public void findsFirstPageOfCities() {

		Page<City> cities = this.repository.findAll(new PageRequest(0, 10));
		assertThat(cities.getTotalElements()).isGreaterThan(20L);
	}

	@Test
	public void findByNameAndCountry() {
		City city = this.repository.findByNameAndCountryAllIgnoringCase("Melbourne",
				"Australia");
		assertThat(city).isNotNull();
		assertThat(city.getName()).isEqualTo("Melbourne");
	}

	@Test
	public void findContaining() {
		Page<City> cities = this.repository
				.findByNameContainingAndCountryContainingAllIgnoringCase("", "UK",
						new PageRequest(0, 10));
		assertThat(cities.getTotalElements()).isEqualTo(3L);
	}

}
