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

package smoketest.data.jpa.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * ReviewDetails class.
 */
public class ReviewDetails implements Serializable {

	private static final long serialVersionUID = 1L;

	private Rating rating;

	private Date checkInDate;

	private TripType tripType;

	private String title;

	private String details;

	/**
     * Constructs a new ReviewDetails object.
     */
    public ReviewDetails() {
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
     * Sets the trip type for the review details.
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
     * @param title the title of the review
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
