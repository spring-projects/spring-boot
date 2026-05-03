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

package smoketest.data.jdbc;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import org.springframework.data.annotation.Id;

public class Customer {

	@Id
	private @Nullable Long id;

	private @Nullable String firstName;

	private @Nullable LocalDate dateOfBirth;

	public @Nullable Long getId() {
		return this.id;
	}

	public void setId(@Nullable Long id) {
		this.id = id;
	}

	public @Nullable String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(@Nullable String firstName) {
		this.firstName = firstName;
	}

	public @Nullable LocalDate getDateOfBirth() {
		return this.dateOfBirth;
	}

	public void setDateOfBirth(@Nullable LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

}
