/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.sbom;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

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
	private Map<String, Sbom> additional = new LinkedHashMap<>();

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
		private @Nullable String location;

		/**
		 * Media type of the SBOM. If null, the media type will be auto-detected.
		 */
		private @Nullable MimeType mediaType;

		public @Nullable String getLocation() {
			return this.location;
		}

		public void setLocation(@Nullable String location) {
			this.location = location;
		}

		public @Nullable MimeType getMediaType() {
			return this.mediaType;
		}

		public void setMediaType(@Nullable MimeType mediaType) {
			this.mediaType = mediaType;
		}

	}

}
