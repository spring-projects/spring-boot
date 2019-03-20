/*
 * Copyright 2012-2018 the original author or authors.
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

package sample.hibernate52.domain;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.util.Assert;

@Entity
public class Review implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	private Hotel hotel;

	@Column(nullable = false, name = "idx")
	private int index;

	@Column(nullable = false)
	@Enumerated(EnumType.ORDINAL)
	private Rating rating;

	@Column(name = "CHECK_IN_DATE", nullable = false)
	@Temporal(TemporalType.DATE)
	private Date checkInDate;

	@Column(name = "TRIP_TYPE", nullable = false)
	@Enumerated(EnumType.ORDINAL)
	private TripType tripType;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, length = 5000)
	private String details;

	protected Review() {
	}

	public Review(Hotel hotel, int index, ReviewDetails details) {
		Assert.notNull(hotel, "Hotel must not be null");
		Assert.notNull(details, "Details must not be null");
		this.hotel = hotel;
		this.index = index;
		this.rating = details.getRating();
		this.checkInDate = details.getCheckInDate();
		this.tripType = details.getTripType();
		this.title = details.getTitle();
		this.details = details.getDetails();
	}

	public Hotel getHotel() {
		return this.hotel;
	}

	public int getIndex() {
		return this.index;
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
