/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.annotations.NaturalId;

/**
 * Hotel class.
 */
@Entity
public class Hotel implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "hotel_generator", sequenceName = "hotel_sequence", initialValue = 28)
	@GeneratedValue(generator = "hotel_generator")
	private Long id;

	@ManyToOne(optional = false)
	@NaturalId
	private City city;

	@Column(nullable = false)
	@NaturalId
	private String name;

	@Column(nullable = false)
	private String address;

	@Column(nullable = false)
	private String zip;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "hotel")
	private Set<Review> reviews;

	/**
     * Constructs a new instance of the Hotel class.
     * 
     * This constructor is marked as protected, which means it can only be accessed by subclasses of the Hotel class.
     * It is used to create a new Hotel object.
     */
    protected Hotel() {
	}

	/**
     * Creates a new Hotel object with the specified city and name.
     * 
     * @param city the city where the hotel is located
     * @param name the name of the hotel
     */
    public Hotel(City city, String name) {
		this.city = city;
		this.name = name;
	}

	/**
     * Returns the city where the hotel is located.
     * 
     * @return the city where the hotel is located
     */
    public City getCity() {
		return this.city;
	}

	/**
     * Returns the name of the hotel.
     *
     * @return the name of the hotel
     */
    public String getName() {
		return this.name;
	}

	/**
     * Returns the address of the hotel.
     *
     * @return the address of the hotel
     */
    public String getAddress() {
		return this.address;
	}

	/**
     * Returns the zip code of the hotel.
     *
     * @return the zip code of the hotel
     */
    public String getZip() {
		return this.zip;
	}

}
