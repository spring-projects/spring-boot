/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.sbom;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.MimeType;

/**
 * Configuration properties for the SBOM endpoint.
 *
 * @author Moritz Halbritter
 * @since 3.3.0
 */
@ConfigurationProperties("management.endpoint.sbom")
public class SbomProperties {

	/**
	 * Application SBOM configuration.
	 */
	private final Sbom application = new Sbom();

	/**
	 * Additional SBOMs.
	 */
	private Map<String, Sbom> additional = new HashMap<>();

	public Sbom getApplication() {
		return this.application;
	}

	public Map<String, Sbom> getAdditional() {
		return this.additional;
	}

	public void setAdditional(Map<String, Sbom> additional) {
		this.additional = additional;
	}

	public static class Sbom {

		/**
		 * Location to the SBOM. If null, the location will be auto-detected.
		 */
		private String location;

		/**
		 * Media type of the SBOM. If null, the media type will be auto-detected.
		 */
		private MimeType mediaType;

		public String getLocation() {
			return this.location;
		}

		public void setLocation(String location) {
			this.location = location;
		}

		public MimeType getMediaType() {
			return this.mediaType;
		}

		public void setMediaType(MimeType mediaType) {
			this.mediaType = mediaType;
		}

	}

}
