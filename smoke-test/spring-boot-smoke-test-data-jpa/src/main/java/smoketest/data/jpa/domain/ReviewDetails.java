/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.jpa.domain;

import java.io.Serializable;
import java.time.LocalDate;

public class ReviewDetails implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Rating rating;

	private final LocalDate checkInDate;

	private final TripType tripType;

	private final String title;

	private final String details;

	public ReviewDetails(Rating rating, LocalDate checkInDate, TripType tripType, String title, String details) {
		this.rating = rating;
		this.checkInDate = checkInDate;
		this.tripType = tripType;
		this.title = title;
		this.details = details;
	}

	public Rating getRating() {
		return this.rating;
	}

	public LocalDate getCheckInDate() {
		return this.checkInDate;
	}

	public TripType getTripType() {
		return this.tripType;
	}

	public String getTitle() {
		return this.title;
	}

	public String getDetails() {
		return this.details;
	}

}
