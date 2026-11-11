/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.jpa.service;

import smoketest.data.jpa.domain.City;
import smoketest.data.jpa.domain.HotelSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CityService {

	Page<City> findCities(CitySearchCriteria criteria, Pageable pageable);

	City getCity(String name, String country);

	Page<HotelSummary> getHotels(City city, Pageable pageable);

}
