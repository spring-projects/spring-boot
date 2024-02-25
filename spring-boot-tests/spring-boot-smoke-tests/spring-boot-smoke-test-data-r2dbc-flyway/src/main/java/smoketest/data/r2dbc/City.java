/*
 * Copyright 2012-2020 the original author or authors.
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

package smoketest.data.r2dbc;

import org.springframework.data.annotation.Id;

/**
 * City class.
 */
public class City {

	@Id
	private Long id;

	private String name;

	private String state;

	private String country;

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
	 * Returns the ID of the city.
	 * @return the ID of the city
	 */
	public Long getId() {
		return this.id;
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
	 * Returns a string representation of the City object. The string contains the name,
	 * state, and country of the city.
	 * @return a string representation of the City object
	 */
	@Override
	public String toString() {
		return getName() + "," + getState() + "," + getCountry();
	}

}
