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

package org.springframework.boot.context.config;

/**
 * Exception throw if a {@link ConfigDataLocation} is not supported.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.4.0
 */
public class UnsupportedConfigDataLocationException extends ConfigDataException {

	private final String location;

	/**
	 * Create a new {@link UnsupportedConfigDataLocationException} instance.
	 * @param location the unsupported location
	 */
	UnsupportedConfigDataLocationException(String location) {
		super("Unsupported config data location '" + location + "'", null);
		this.location = location;
	}

	/**
	 * Return the unsupported location.
	 * @return the unsupported location
	 */
	public String getLocation() {
		return this.location;
	}

}
