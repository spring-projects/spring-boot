package org.springframework.bootstrap.sample.data.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.sample.data.domain.City;
import org.springframework.bootstrap.sample.data.domain.Hotel;
import org.springframework.bootstrap.sample.data.domain.Rating;
import org.springframework.bootstrap.sample.data.domain.RatingCount;
import org.springframework.bootstrap.sample.data.domain.Review;
import org.springframework.bootstrap.sample.data.domain.ReviewDetails;
import org.springframework.bootstrap.sample.data.domain.repository.HotelRepository;
import org.springframework.bootstrap.sample.data.domain.repository.HotelSummaryRepository;
import org.springframework.bootstrap.sample.data.domain.repository.ReviewRepository;
import org.springframework.bootstrap.sample.data.service.HotelService;
import org.springframework.bootstrap.sample.data.service.ReviewsSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Component("hotelService")
@Transactional
public class HotelServiceImpl implements HotelService {

	// FIXME deal with null repository return values

	private HotelRepository hotelRepository;

	private HotelSummaryRepository hotelSummaryRepository;

	private ReviewRepository reviewRepository;

	@Override
	public Hotel getHotel(City city, String name) {
		Assert.notNull(city, "City must not be null");
		Assert.hasLength(name, "Name must not be empty");
		return this.hotelRepository.findByCityAndName(city, name);
	}

	@Override
	public Page<Review> getReviews(Hotel hotel, Pageable pageable) {
		Assert.notNull(hotel, "Hotel must not be null");
		return this.reviewRepository.findByHotel(hotel, pageable);
	}

	@Override
	public Review getReview(Hotel hotel, int reviewNumber) {
		Assert.notNull(hotel, "Hotel must not be null");
		return this.reviewRepository.findByHotelAndIndex(hotel, reviewNumber);
	}

	@Override
	public Review addReview(Hotel hotel, ReviewDetails details) {
		// FIXME
		System.out.println(details.getTitle());
		return new Review(hotel, 1, details);
	}

	@Override
	public ReviewsSummary getReviewSummary(Hotel hotel) {
		List<RatingCount> ratingCounts = this.hotelSummaryRepository
				.findRatingCounts(hotel);
		return new ReviewsSummaryImpl(ratingCounts);
	}

	@Autowired
	public void setHotelRepository(HotelRepository hotelRepository) {
		this.hotelRepository = hotelRepository;
	}

	@Autowired
	public void setHotelSummaryRepository(HotelSummaryRepository hotelSummaryRepository) {
		this.hotelSummaryRepository = hotelSummaryRepository;
	}

	@Autowired
	public void setReviewRepository(ReviewRepository reviewRepository) {
		this.reviewRepository = reviewRepository;
	}

	private static class ReviewsSummaryImpl implements ReviewsSummary {

		private Map<Rating, Long> ratingCount;

		public ReviewsSummaryImpl(List<RatingCount> ratingCounts) {
			this.ratingCount = new HashMap<Rating, Long>();
			for (RatingCount ratingCount : ratingCounts) {
				this.ratingCount.put(ratingCount.getRating(), ratingCount.getCount());
			}
		}

		@Override
		public long getNumberOfReviewsWithRating(Rating rating) {
			Long count = this.ratingCount.get(rating);
			return count == null ? 0 : count;
		}
	}
}
