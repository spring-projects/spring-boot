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

package org.springframework.boot.autoconfigure.flyway;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when no Flyway migration script is available.
 *
 * @author Anand Shastri
 * @author Stephane Nicoll
 * @since 2.2.0
 */
public class FlywayMigrationScriptMissingException extends RuntimeException {

	private final List<String> locations;

	FlywayMigrationScriptMissingException(List<String> locations) {
		super(locations.isEmpty() ? "Migration script locations not configured"
				: "Cannot find migrations location in: " + locations
						+ " (please add migrations or check your Flyway configuration)");
		this.locations = new ArrayList<>(locations);
	}

	public List<String> getLocations() {
		return this.locations;
	}

}
