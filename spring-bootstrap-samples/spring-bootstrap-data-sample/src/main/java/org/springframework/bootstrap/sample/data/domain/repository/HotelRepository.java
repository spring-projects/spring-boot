package org.springframework.bootstrap.sample.data.domain.repository;

import org.springframework.bootstrap.sample.data.domain.City;
import org.springframework.bootstrap.sample.data.domain.Hotel;
import org.springframework.data.repository.Repository;

public interface HotelRepository extends Repository<Hotel, Long> {

	Hotel findByCityAndName(City city, String name);

}
