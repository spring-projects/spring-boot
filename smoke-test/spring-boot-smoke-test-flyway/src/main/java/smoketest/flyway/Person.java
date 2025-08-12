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

package smoketest.flyway;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import org.jspecify.annotations.Nullable;

@Entity
public class Person {

	@Id
	@SequenceGenerator(name = "person_generator", sequenceName = "person_sequence", allocationSize = 1)
	@GeneratedValue(generator = "person_generator")
	private @Nullable Long id;

	private @Nullable String firstName;

	private @Nullable String lastName;

	public @Nullable String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(@Nullable String firstName) {
		this.firstName = firstName;
	}

	public @Nullable String getLastName() {
		return this.lastName;
	}

	public void setLastName(@Nullable String lastname) {
		this.lastName = lastname;
	}

	@Override
	public String toString() {
		return "Person [firstName=" + this.firstName + ", lastName=" + this.lastName + "]";
	}

}
