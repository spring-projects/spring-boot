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

package org.springframework.boot.docs.data.sql.jpaandspringdata.entityclasses;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * City class.
 */
@Entity
public class City implements Serializable {

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String state;

	// ... additional members, often include @OneToMany mappings

	protected City() {
		// no-args constructor required by JPA spec
		// this one is protected since it should not be used directly
	}

	/**
	 * Constructs a new City object with the specified name and state.
	 * @param name the name of the city
	 * @param state the state in which the city is located
	 */
	public City(String name, String state) {
		this.name = name;
		this.state = state;
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

	// ... etc

}
