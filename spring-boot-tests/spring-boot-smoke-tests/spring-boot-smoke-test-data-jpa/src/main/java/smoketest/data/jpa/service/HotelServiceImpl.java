/*
 * Copyright 2012-2019 the original author or authors.
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

package smoketest.data.jpa.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import smoketest.data.jpa.domain.City;
import smoketest.data.jpa.domain.Hotel;
import smoketest.data.jpa.domain.Rating;
import smoketest.data.jpa.domain.RatingCount;
import smoketest.data.jpa.domain.Review;
import smoketest.data.jpa.domain.ReviewDetails;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * HotelServiceImpl class.
 */
@Component("hotelService")
@Transactional
class HotelServiceImpl implements HotelService {

	private final HotelRepository hotelRepository;

	private final ReviewRepository reviewRepository;

	/**
	 * Constructs a new HotelServiceImpl with the specified HotelRepository and
	 * ReviewRepository.
	 * @param hotelRepository the repository for managing hotel data
	 * @param reviewRepository the repository for managing review data
	 */
	HotelServiceImpl(HotelRepository hotelRepository, ReviewRepository reviewRepository) {
		this.hotelRepository = hotelRepository;
		this.reviewRepository = reviewRepository;
	}

	/**
	 * Retrieves a hotel based on the given city and name.
	 * @param city the city where the hotel is located (must not be null)
	 * @param name the name of the hotel (must not be empty)
	 * @return the hotel matching the given city and name, or null if not found
	 * @throws IllegalArgumentException if the city is null or the name is empty
	 */
	@Override
	public Hotel getHotel(City city, String name) {
		Assert.notNull(city, "City must not be null");
		Assert.hasLength(name, "Name must not be empty");
		return this.hotelRepository.findByCityAndName(city, name);
	}

	/**
	 * Retrieves a page of reviews for a given hotel.
	 * @param hotel the hotel for which to retrieve the reviews (must not be null)
	 * @param pageable the pagination information for the result set
	 * @return a page of reviews for the given hotel
	 * @throws IllegalArgumentException if the hotel is null
	 */
	@Override
	public Page<Review> getReviews(Hotel hotel, Pageable pageable) {
		Assert.notNull(hotel, "Hotel must not be null");
		return this.reviewRepository.findByHotel(hotel, pageable);
	}

	/**
	 * Retrieves a specific review for a given hotel.
	 * @param hotel The hotel for which the review is requested. Must not be null.
	 * @param reviewNumber The index of the review to retrieve.
	 * @return The review object corresponding to the given hotel and review number.
	 * @throws IllegalArgumentException if the hotel is null.
	 */
	@Override
	public Review getReview(Hotel hotel, int reviewNumber) {
		Assert.notNull(hotel, "Hotel must not be null");
		return this.reviewRepository.findByHotelAndIndex(hotel, reviewNumber);
	}

	/**
	 * Adds a new review for a hotel.
	 * @param hotel the hotel for which the review is being added
	 * @param details the details of the review
	 * @return the newly added review
	 */
	@Override
	public Review addReview(Hotel hotel, ReviewDetails details) {
		Review review = new Review(hotel, 1, details);
		return this.reviewRepository.save(review);
	}

	/**
	 * Retrieves the review summary for a given hotel.
	 * @param hotel the hotel for which to retrieve the review summary
	 * @return the review summary for the given hotel
	 */
	@Override
	public ReviewsSummary getReviewSummary(Hotel hotel) {
		List<RatingCount> ratingCounts = this.hotelRepository.findRatingCounts(hotel);
		return new ReviewsSummaryImpl(ratingCounts);
	}

	/**
	 * ReviewsSummaryImpl class.
	 */
	private static class ReviewsSummaryImpl implements ReviewsSummary {

		private final Map<Rating, Long> ratingCount;

		/**
		 * Constructs a new ReviewsSummaryImpl object with the given list of RatingCount
		 * objects.
		 * @param ratingCounts the list of RatingCount objects to be used for constructing
		 * the ReviewsSummaryImpl object
		 */
		ReviewsSummaryImpl(List<RatingCount> ratingCounts) {
			this.ratingCount = new HashMap<>();
			for (RatingCount ratingCount : ratingCounts) {
				this.ratingCount.put(ratingCount.getRating(), ratingCount.getCount());
			}
		}

		/**
		 * Returns the number of reviews with the specified rating.
		 * @param rating the rating to count the number of reviews for
		 * @return the number of reviews with the specified rating
		 */
		@Override
		public long getNumberOfReviewsWithRating(Rating rating) {
			Long count = this.ratingCount.get(rating);
			return (count != null) ? count : 0;
		}

	}

}
