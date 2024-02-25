/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * {@link ConfigurationProperties Properties} for Spring WebFlux.
 *
 * @author Brian Clozel
 * @author Vedran Pavic
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.webflux")
public class WebFluxProperties {

	/**
	 * Base path for all web handlers.
	 */
	private String basePath;

	private final Format format = new Format();

	private final Problemdetails problemdetails = new Problemdetails();

	/**
	 * Path pattern used for static resources.
	 */
	private String staticPathPattern = "/**";

	/**
	 * Path pattern used for WebJar assets.
	 */
	private String webjarsPathPattern = "/webjars/**";

	/**
	 * Returns the base path of the WebFlux application.
	 * @return the base path of the WebFlux application
	 */
	public String getBasePath() {
		return this.basePath;
	}

	/**
	 * Sets the base path for the WebFluxProperties.
	 * @param basePath the base path to be set
	 */
	public void setBasePath(String basePath) {
		this.basePath = cleanBasePath(basePath);
	}

	/**
	 * Cleans the base path by removing leading and trailing whitespaces and ensuring it
	 * starts with a forward slash and does not end with a forward slash.
	 * @param basePath the base path to be cleaned
	 * @return the cleaned base path
	 */
	private String cleanBasePath(String basePath) {
		String candidate = null;
		if (StringUtils.hasLength(basePath)) {
			candidate = basePath.strip();
		}
		if (StringUtils.hasText(candidate)) {
			if (!candidate.startsWith("/")) {
				candidate = "/" + candidate;
			}
			if (candidate.endsWith("/")) {
				candidate = candidate.substring(0, candidate.length() - 1);
			}
		}
		return candidate;
	}

	/**
	 * Returns the format of the WebFluxProperties.
	 * @return the format of the WebFluxProperties
	 */
	public Format getFormat() {
		return this.format;
	}

	/**
	 * Returns the Problemdetails object associated with this WebFluxProperties instance.
	 * @return the Problemdetails object
	 */
	public Problemdetails getProblemdetails() {
		return this.problemdetails;
	}

	/**
	 * Returns the static path pattern.
	 * @return the static path pattern
	 */
	public String getStaticPathPattern() {
		return this.staticPathPattern;
	}

	/**
	 * Sets the static path pattern for the WebFluxProperties.
	 * @param staticPathPattern the static path pattern to be set
	 */
	public void setStaticPathPattern(String staticPathPattern) {
		this.staticPathPattern = staticPathPattern;
	}

	/**
	 * Returns the webjars path pattern.
	 * @return the webjars path pattern
	 */
	public String getWebjarsPathPattern() {
		return this.webjarsPathPattern;
	}

	/**
	 * Sets the path pattern for WebJars resources.
	 * @param webjarsPathPattern the path pattern for WebJars resources
	 */
	public void setWebjarsPathPattern(String webjarsPathPattern) {
		this.webjarsPathPattern = webjarsPathPattern;
	}

	/**
	 * Format class.
	 */
	public static class Format {

		/**
		 * Date format to use, for example 'dd/MM/yyyy'.
		 */
		private String date;

		/**
		 * Time format to use, for example 'HH:mm:ss'.
		 */
		private String time;

		/**
		 * Date-time format to use, for example 'yyyy-MM-dd HH:mm:ss'.
		 */
		private String dateTime;

		/**
		 * Returns the date.
		 * @return the date
		 */
		public String getDate() {
			return this.date;
		}

		/**
		 * Sets the date for the Format class.
		 * @param date the date to be set
		 */
		public void setDate(String date) {
			this.date = date;
		}

		/**
		 * Returns the current time.
		 * @return the current time as a String
		 */
		public String getTime() {
			return this.time;
		}

		/**
		 * Sets the time value for the Format object.
		 * @param time the time value to be set
		 */
		public void setTime(String time) {
			this.time = time;
		}

		/**
		 * Returns the date and time in the specified format.
		 * @return the date and time in the specified format
		 */
		public String getDateTime() {
			return this.dateTime;
		}

		/**
		 * Sets the date and time for the Format object.
		 * @param dateTime the date and time to be set
		 */
		public void setDateTime(String dateTime) {
			this.dateTime = dateTime;
		}

	}

	/**
	 * Problemdetails class.
	 */
	public static class Problemdetails {

		/**
		 * Whether RFC 7807 Problem Details support should be enabled.
		 */
		private boolean enabled = false;

		/**
		 * Returns the current status of the enabled flag.
		 * @return {@code true} if the flag is enabled, {@code false} otherwise.
		 */
		public boolean isEnabled() {
			return this.enabled;
		}

		/**
		 * Sets the enabled status of the Problemdetails object.
		 * @param enabled the boolean value indicating whether the Problemdetails object
		 * is enabled or not
		 */
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}
