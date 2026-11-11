/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.r2dbc;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CityController {

	private final CityRepository repository;

	public CityController(CityRepository repository) {
		this.repository = repository;
	}

	@GetMapping("/cities")
	public Flux<City> findCities() {
		return this.repository.findAll();
	}

	@GetMapping("/cities/{id}")
	public Mono<City> findCityById(@PathVariable long id) {
		return this.repository.findById(id);
	}

}
