package org.springframework.bootstrap.sample.data.service;

import org.springframework.bootstrap.sample.data.domain.City;
import org.springframework.bootstrap.sample.data.domain.HotelSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CityService {

	Page<City> findCities(CitySearchCriteria criteria, Pageable pageable);

	City getCity(String name, String country);

	Page<HotelSummary> getHotels(City city, Pageable pageable);

}
