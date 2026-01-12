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

package smoketest.data.r2dbc;

import org.jspecify.annotations.Nullable;

import org.springframework.data.annotation.Id;

public class City {

	@Id
	private @Nullable Long id;

	@SuppressWarnings("NullAway.Init")
	private String name;

	@SuppressWarnings("NullAway.Init")
	private String state;

	@SuppressWarnings("NullAway.Init")
	private String country;

	protected City() {
	}

	public City(String name, String state, String country) {
		this.name = name;
		this.state = state;
		this.country = country;
	}

	public @Nullable Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String getState() {
		return this.state;
	}

	public String getCountry() {
		return this.country;
	}

	@Override
	public String toString() {
		return getName() + "," + getState() + "," + getCountry();
	}

}
