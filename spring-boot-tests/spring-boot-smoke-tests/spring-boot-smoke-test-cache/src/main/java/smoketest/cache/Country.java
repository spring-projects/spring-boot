/*
 * Copyright 2012-2019 the original author or authors.
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

package smoketest.cache;

import java.io.Serializable;

/**
 * Country class.
 */
@SuppressWarnings("serial")
public class Country implements Serializable {

	private final String code;

	/**
	 * Constructs a new Country object with the specified code.
	 * @param code the code of the country
	 */
	public Country(String code) {
		this.code = code;
	}

	/**
	 * Returns the code of the country.
	 * @return the code of the country
	 */
	public String getCode() {
		return this.code;
	}

	/**
	 * Compares this Country object to the specified object. The result is true if and
	 * only if the argument is not null and is a Country object that has the same code as
	 * this object.
	 * @param o the object to compare this Country against
	 * @return true if the given object represents a Country with the same code, false
	 * otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Country country = (Country) o;

		return this.code.equals(country.code);
	}

	/**
	 * Returns the hash code value for this Country object.
	 * @return the hash code value for this object
	 */
	@Override
	public int hashCode() {
		return this.code.hashCode();
	}

}
