/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.jpa.service;

import smoketest.data.jpa.domain.Hotel;
import smoketest.data.jpa.domain.Review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

interface ReviewRepository extends Repository<Review, Long> {

	Page<Review> findByHotel(Hotel hotel, Pageable pageable);

	Review findByHotelAndIndex(Hotel hotel, int index);

	Review save(Review review);

}
