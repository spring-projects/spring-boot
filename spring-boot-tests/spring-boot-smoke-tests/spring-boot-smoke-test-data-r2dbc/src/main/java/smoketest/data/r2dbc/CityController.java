/*
 * Copyright 2012-2020 the original author or authors.
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

package smoketest.data.r2dbc;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * CityController class.
 */
@RestController
public class CityController {

	private final CityRepository repository;

	/**
     * Constructs a new CityController with the specified CityRepository.
     * 
     * @param repository the CityRepository to be used by the CityController
     */
    public CityController(CityRepository repository) {
		this.repository = repository;
	}

	/**
     * Retrieves all cities.
     *
     * @return a Flux of City objects representing all cities.
     */
    @GetMapping("/cities")
	public Flux<City> findCities() {
		return this.repository.findAll();
	}

	/**
     * Retrieves a city by its ID.
     *
     * @param id the ID of the city to retrieve
     * @return a Mono containing the city with the specified ID, or an empty Mono if no city is found
     */
    @GetMapping("/cities/{id}")
	public Mono<City> findCityById(@PathVariable long id) {
		return this.repository.findById(id);
	}

}
