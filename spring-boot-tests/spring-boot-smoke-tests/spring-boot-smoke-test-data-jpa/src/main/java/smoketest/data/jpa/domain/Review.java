/*
 * Copyright 2012-2021 the original author or authors.
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

package smoketest.data.jpa.domain;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.springframework.util.Assert;

/**
 * Review class.
 */
@Entity
public class Review implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "review_generator", sequenceName = "review_sequence", initialValue = 64)
	@GeneratedValue(generator = "review_generator")
	private Long id;

	@ManyToOne(optional = false)
	private Hotel hotel;

	@Column(nullable = false, name = "idx")
	private int index;

	@Column(nullable = false)
	@Enumerated(EnumType.ORDINAL)
	private Rating rating;

	@Column(nullable = false)
	@Temporal(TemporalType.DATE)
	private Date checkInDate;

	@Column(nullable = false)
	@Enumerated(EnumType.ORDINAL)
	private TripType tripType;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, length = 5000)
	private String details;

	/**
     * Creates a new instance of the Review class.
     */
    protected Review() {
	}

	/**
     * Creates a new Review object with the given hotel, index, and review details.
     * 
     * @param hotel the hotel associated with the review (must not be null)
     * @param index the index of the review
     * @param details the review details (must not be null)
     * @throws IllegalArgumentException if hotel or details is null
     */
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

	/**
     * Returns the hotel associated with this review.
     *
     * @return the hotel associated with this review
     */
    public Hotel getHotel() {
		return this.hotel;
	}

	/**
     * Returns the index value.
     *
     * @return the index value
     */
    public int getIndex() {
		return this.index;
	}

	/**
     * Returns the rating of the review.
     *
     * @return the rating of the review
     */
    public Rating getRating() {
		return this.rating;
	}

	/**
     * Sets the rating for the review.
     * 
     * @param rating the rating to be set
     */
    public void setRating(Rating rating) {
		this.rating = rating;
	}

	/**
     * Returns the check-in date of the review.
     *
     * @return the check-in date of the review
     */
    public Date getCheckInDate() {
		return this.checkInDate;
	}

	/**
     * Sets the check-in date for the review.
     * 
     * @param checkInDate the check-in date to be set
     */
    public void setCheckInDate(Date checkInDate) {
		this.checkInDate = checkInDate;
	}

	/**
     * Returns the trip type of the review.
     * 
     * @return the trip type of the review
     */
    public TripType getTripType() {
		return this.tripType;
	}

	/**
     * Sets the trip type for the review.
     * 
     * @param tripType the trip type to be set
     */
    public void setTripType(TripType tripType) {
		this.tripType = tripType;
	}

	/**
     * Returns the title of the review.
     *
     * @return the title of the review
     */
    public String getTitle() {
		return this.title;
	}

	/**
     * Sets the title of the review.
     * 
     * @param title the title to be set
     */
    public void setTitle(String title) {
		this.title = title;
	}

	/**
     * Returns the details of the review.
     *
     * @return the details of the review
     */
    public String getDetails() {
		return this.details;
	}

	/**
     * Sets the details of the review.
     * 
     * @param details the details of the review
     */
    public void setDetails(String details) {
		this.details = details;
	}

}
