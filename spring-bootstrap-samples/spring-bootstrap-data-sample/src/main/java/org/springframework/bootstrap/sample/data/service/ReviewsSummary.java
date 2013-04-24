package org.springframework.bootstrap.sample.data.service;

import org.springframework.bootstrap.sample.data.domain.Rating;

public interface ReviewsSummary {

	public long getNumberOfReviewsWithRating(Rating rating);

}
