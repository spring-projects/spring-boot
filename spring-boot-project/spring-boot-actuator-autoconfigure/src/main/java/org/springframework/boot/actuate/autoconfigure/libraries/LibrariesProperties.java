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

package org.springframework.boot.actuate.autoconfigure.libraries;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Configuration properties for core libraries contributors.
 *
 * @author Phil Clay
 * @since 2.5.0
 */
@ConfigurationProperties(LibrariesProperties.CONFIGURATION_PROPERTIES_PREFIX)
public class LibrariesProperties {

	static final String CONFIGURATION_PROPERTIES_PREFIX = "management.libraries";

	private final Bundled bundled = new Bundled();

	/**
	 * Gets the properties for exposing the libraries bundled in the application archive,
	 * typically by the spring boot maven or gradle plugins.
	 * @return the properties for exposing the libraries bundled in the application
	 * archive
	 */
	public Bundled getBundled() {
		return this.bundled;
	}

	/**
	 * Properties for exposing the libraries bundled in the application archive, typically
	 * by the spring boot maven or gradle plugins.
	 */
	public static class Bundled {

		/**
		 * The default classpath location of the {@code bundled-libraries.yaml} file. This
		 * location is where the spring boot maven and gradle plugins will write the
		 * {@code bundled-libraries.yaml} file within the jar/war.
		 */
		public static final String DEFAULT_CLASSPATH_LOCATION = "META-INF/bundled-libraries.yaml";

		/**
		 * Location of the generated {@code bundled-libraries.yaml} file.
		 */
		private Resource location = new ClassPathResource(DEFAULT_CLASSPATH_LOCATION);

		/**
		 * Gets the location of the generated {@code bundled-libraries.yaml} file.
		 *
		 * <p>
		 * Defaults to {@value #DEFAULT_CLASSPATH_LOCATION}.
		 * </p>
		 * @return the location of the generated {@code bundled-libraries.yaml} file.
		 */
		public Resource getLocation() {
			return this.location;
		}

		/**
		 * Sets the location of the generated {@code bundled-libraries.yaml} file.
		 * <p>
		 * Defaults to {@value #DEFAULT_CLASSPATH_LOCATION}.
		 * </p>
		 * @param location the location of the generated {@code bundled-libraries.yaml}
		 * file.
		 */
		public void setLocation(Resource location) {
			this.location = location;
		}

	}

}
