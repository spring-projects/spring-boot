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

package smoketest.test.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.springframework.util.Assert;

/**
 * A user of the system.
 *
 * @author Phillip Webb
 */
@Entity
@Table(name = "DRIVER")
public class User {

	@Id
	@GeneratedValue
	private Long id;

	@Column(unique = true)
	private String username;

	private VehicleIdentificationNumber vin;

	protected User() {
	}

	public User(String username, VehicleIdentificationNumber vin) {
		Assert.hasLength(username, "Username must not be empty");
		Assert.notNull(vin, "VIN must not be null");
		this.username = username;
		this.vin = vin;
	}

	public Long getId() {
		return this.id;
	}

	public String getUsername() {
		return this.username;
	}

	public VehicleIdentificationNumber getVin() {
		return this.vin;
	}

}
