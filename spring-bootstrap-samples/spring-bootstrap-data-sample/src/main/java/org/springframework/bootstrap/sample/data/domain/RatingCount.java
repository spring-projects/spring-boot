package org.springframework.bootstrap.sample.data.domain;

import java.io.Serializable;

public class RatingCount implements Serializable {

	private static final long serialVersionUID = 1L;

	private Rating rating;

	private long count;

	public RatingCount(Rating rating, long count) {
		this.rating = rating;
		this.count = count;
	}

	public Rating getRating() {
		return this.rating;
	}

	public long getCount() {
		return this.count;
	}
}
