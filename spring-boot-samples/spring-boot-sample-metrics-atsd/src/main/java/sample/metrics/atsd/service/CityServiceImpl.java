/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.metrics.atsd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sample.metrics.atsd.Measured;
import sample.metrics.atsd.domain.City;
import sample.metrics.atsd.repository.CityRepository;

import java.util.List;

@Service
public class CityServiceImpl implements CityService {
	public static final String CITIES_CREATED_METRIC = "cities-created";
	public static final String CITIES_REMOVED_METRIC = "cities-removed";
	private CityRepository cityRepository;
	private CounterService counterService;

	@Autowired
	public void setCityRepository(CityRepository cityRepository) {
		this.cityRepository = cityRepository;
	}

	@Autowired
	public void setCounterService(CounterService counterService) {
		this.counterService = counterService;
	}

	@Override
	@Transactional(readOnly = true)
	@Measured
	public List<City> findCities() {
		return this.cityRepository.findAll();
	}

	@Override
	@Cacheable("city-cache")
	@Transactional(readOnly = true)
	public City getCity(Long id) {
		try {
			return this.cityRepository.findById(id);
		} catch (EmptyResultDataAccessException e) {
			throw new IllegalArgumentException("City not found for id=" + id);
		}
	}

	@Override
	@Transactional
	public void createCity(City city) {
		this.cityRepository.save(city);
		this.counterService.increment(CITIES_CREATED_METRIC);
	}

	@Override
	@Transactional
	@CacheEvict("city-cache")
	public void deleteCity(Long id) {
		this.cityRepository.remove(id);
		this.counterService.increment(CITIES_REMOVED_METRIC);
	}
}
