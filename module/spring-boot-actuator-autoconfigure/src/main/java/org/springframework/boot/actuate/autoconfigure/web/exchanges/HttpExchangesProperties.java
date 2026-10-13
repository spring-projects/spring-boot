/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.web.exchanges;

import java.util.LinkedHashSet;
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
@ConfigurationProperties("management.httpexchanges")
public class HttpExchangesProperties {

	private final Recording recording = new Recording();

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
		private Set<Include> include = new LinkedHashSet<>(Include.defaultIncludes());

		public Set<Include> getInclude() {
			return this.include;
		}

		public void setInclude(Set<Include> include) {
			this.include = include;
		}

	}

}
