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

package org.springframework.boot.data.cassandra.domain.city;

import org.jspecify.annotations.Nullable;

import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.CassandraType.Name;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table
public class City {

	@PrimaryKey
	@CassandraType(type = Name.BIGINT)
	private @Nullable Long id;

	@Column
	private @Nullable String name;

	@Column
	private @Nullable String state;

	@Column
	private @Nullable String country;

	@Column
	private @Nullable String map;

	public @Nullable Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public @Nullable String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public @Nullable String getState() {
		return this.state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public @Nullable String getCountry() {
		return this.country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public @Nullable String getMap() {
		return this.map;
	}

	public void setMap(String map) {
		this.map = map;
	}

}
