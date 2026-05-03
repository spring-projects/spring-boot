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

package smoketest.data.rest.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.annotations.NaturalId;
import org.jspecify.annotations.Nullable;

@Entity
public class Hotel implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "hotel_generator", sequenceName = "hotel_sequence", initialValue = 28)
	@GeneratedValue(generator = "hotel_generator")
	private @Nullable Long id;

	@ManyToOne(optional = false)
	@NaturalId
	@SuppressWarnings("NullAway.Init")
	private City city;

	@Column(nullable = false)
	@NaturalId
	@SuppressWarnings("NullAway.Init")
	private String name;

	@Column(nullable = false)
	@SuppressWarnings("NullAway.Init")
	private String address;

	@Column(nullable = false)
	@SuppressWarnings("NullAway.Init")
	private String zip;

	protected Hotel() {
	}

	public Hotel(City city, String name, String address, String zip) {
		this.city = city;
		this.name = name;
		this.address = address;
		this.zip = zip;
	}

	public City getCity() {
		return this.city;
	}

	public String getName() {
		return this.name;
	}

	public String getAddress() {
		return this.address;
	}

	public String getZip() {
		return this.zip;
	}

}
