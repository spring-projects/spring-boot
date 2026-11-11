/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.jpa.service;

import java.util.List;

import smoketest.data.jpa.domain.City;
import smoketest.data.jpa.domain.Hotel;
import smoketest.data.jpa.domain.HotelSummary;
import smoketest.data.jpa.domain.RatingCount;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface HotelRepository extends Repository<Hotel, Long> {

	Hotel findByCityAndName(City city, String name);

	@Query("select h.city as city, h.name as name, avg(cast(r.rating as Integer)) as averageRating "
			+ "from Hotel h left outer join h.reviews r where h.city = ?1 group by h")
	Page<HotelSummary> findByCity(City city, Pageable pageable);

	@Query("select r.rating as rating, count(r) as count "
			+ "from Review r where r.hotel = ?1 group by r.rating order by r.rating DESC")
	List<RatingCount> findRatingCounts(Hotel hotel);

}
