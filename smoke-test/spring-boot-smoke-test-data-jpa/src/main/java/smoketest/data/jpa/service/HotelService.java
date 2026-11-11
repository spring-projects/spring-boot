/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.jpa.service;

import smoketest.data.jpa.domain.City;
import smoketest.data.jpa.domain.Hotel;
import smoketest.data.jpa.domain.Review;
import smoketest.data.jpa.domain.ReviewDetails;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HotelService {

	Hotel getHotel(City city, String name);

	Page<Review> getReviews(Hotel hotel, Pageable pageable);

	Review getReview(Hotel hotel, int index);

	Review addReview(Hotel hotel, ReviewDetails details);

	ReviewsSummary getReviewSummary(Hotel hotel);

}
