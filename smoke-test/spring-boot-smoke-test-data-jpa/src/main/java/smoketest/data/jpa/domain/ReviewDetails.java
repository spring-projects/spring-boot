/*
 * Copyright 2012-present the original author or authors.
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

public class ReviewDetails implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Rating rating;

	private final Date checkInDate;

	private final TripType tripType;

	private final String title;

	private final String details;

	public ReviewDetails(Rating rating, Date checkInDate, TripType tripType, String title, String details) {
		this.rating = rating;
		this.checkInDate = checkInDate;
		this.tripType = tripType;
		this.title = title;
		this.details = details;
	}

	public Rating getRating() {
		return this.rating;
	}

	public Date getCheckInDate() {
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
