package org.springframework.bootstrap.sample.data.service;

import org.springframework.bootstrap.sample.data.domain.City;
import org.springframework.bootstrap.sample.data.domain.Hotel;
import org.springframework.bootstrap.sample.data.domain.Review;
import org.springframework.bootstrap.sample.data.domain.ReviewDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HotelService {

	Hotel getHotel(City city, String name);

	Page<Review> getReviews(Hotel hotel, Pageable pageable);

	Review getReview(Hotel hotel, int index);

	Review addReview(Hotel hotel, ReviewDetails details);

	ReviewsSummary getReviewSummary(Hotel hotel);

}
