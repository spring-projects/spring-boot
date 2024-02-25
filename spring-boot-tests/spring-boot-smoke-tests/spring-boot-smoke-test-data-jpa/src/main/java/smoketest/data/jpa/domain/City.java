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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

/**
 * City class.
 */
@Entity
public class City implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "city_generator", sequenceName = "city_sequence", initialValue = 23)
	@GeneratedValue(generator = "city_generator")
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String state;

	@Column(nullable = false)
	private String country;

	@Column(nullable = false)
	private String map;

	/**
	 * Constructs a new instance of the City class.
	 *
	 * This constructor is protected to prevent direct instantiation of the City class. It
	 * can only be accessed by subclasses within the same package or by classes that
	 * inherit from the City class.
	 */
	protected City() {
	}

	/**
	 * Constructs a new City object with the specified name and country.
	 * @param name the name of the city
	 * @param country the country where the city is located
	 */
	public City(String name, String country) {
		this.name = name;
		this.country = country;
	}

	/**
	 * Returns the name of the city.
	 * @return the name of the city
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the state of the city.
	 * @return the state of the city
	 */
	public String getState() {
		return this.state;
	}

	/**
	 * Returns the country of the city.
	 * @return the country of the city
	 */
	public String getCountry() {
		return this.country;
	}

	/**
	 * Returns the map of the city.
	 * @return the map of the city
	 */
	public String getMap() {
		return this.map;
	}

	/**
	 * Returns a string representation of the City object. The string contains the name,
	 * state, and country of the city.
	 * @return a string representation of the City object
	 */
	@Override
	public String toString() {
		return getName() + "," + getState() + "," + getCountry();
	}

}
