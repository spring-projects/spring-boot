package org.springframework.bootstrap.sample.data.domain;

import java.io.Serializable;
import java.util.Date;

//FIXME Hibernate bug HHH-5792 prevents this being embedded

public class ReviewDetails implements Serializable {

	private static final long serialVersionUID = 1L;

	private Rating rating;

	private Date checkInDate;

	private TripType tripType;

	private String title;

	private String details;

	public ReviewDetails() {
	}

	@Deprecated
	public ReviewDetails(String title, Rating rating) {
		this.title = title;
		this.rating = rating;
	}

	public Rating getRating() {
		return this.rating;
	}

	public void setRating(Rating rating) {
		this.rating = rating;
	}

	public Date getCheckInDate() {
		return this.checkInDate;
	}

	public void setCheckInDate(Date checkInDate) {
		this.checkInDate = checkInDate;
	}

	public TripType getTripType() {
		return this.tripType;
	}

	public void setTripType(TripType tripType) {
		this.tripType = tripType;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDetails() {
		return this.details;
	}

	public void setDetails(String details) {
		this.details = details;
	}
}
