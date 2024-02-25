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

package org.springframework.boot.actuate.autoconfigure.web.exchanges;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.actuate.web.exchanges.Include;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for recording HTTP exchanges.
 *
 * @author Wallace Wadge
 * @author Phillip Webb
 * @author Venil Noronha
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.httpexchanges")
public class HttpExchangesProperties {

	private final Recording recording = new Recording();

	/**
     * Returns the recording object associated with this HttpExchangesProperties instance.
     *
     * @return the recording object
     */
    public Recording getRecording() {
		return this.recording;
	}

	/**
	 * Recording properties.
	 *
	 * @since 3.0.0
	 */
	public static class Recording {

		/**
		 * Items to be included in the exchange recording. Defaults to request headers
		 * (excluding Authorization and Cookie), response headers (excluding Set-Cookie),
		 * and time taken.
		 */
		private Set<Include> include = new HashSet<>(Include.defaultIncludes());

		/**
         * Returns the set of includes for this recording.
         *
         * @return the set of includes for this recording
         */
        public Set<Include> getInclude() {
			return this.include;
		}

		/**
         * Sets the include set for the recording.
         * 
         * @param include the set of includes to be set
         */
        public void setInclude(Set<Include> include) {
			this.include = include;
		}

	}

}
