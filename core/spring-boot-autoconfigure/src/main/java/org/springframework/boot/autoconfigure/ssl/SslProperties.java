/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.ssl;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for centralized SSL trust material configuration.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @since 3.1.0
 */
@ConfigurationProperties("spring.ssl")
public class SslProperties {

	/**
	 * SSL bundles.
	 */
	private final Bundles bundle = new Bundles();

	public Bundles getBundle() {
		return this.bundle;
	}

	/**
	 * Properties to define SSL Bundles.
	 */
	public static class Bundles {

		/**
		 * PEM-encoded SSL trust material.
		 */
		private final Map<String, PemSslBundleProperties> pem = new LinkedHashMap<>();

		/**
		 * Java keystore SSL trust material.
		 */
		private final Map<String, JksSslBundleProperties> jks = new LinkedHashMap<>();

		/**
		 * Trust material watching.
		 */
		private final Watch watch = new Watch();

		public Map<String, PemSslBundleProperties> getPem() {
			return this.pem;
		}

		public Map<String, JksSslBundleProperties> getJks() {
			return this.jks;
		}

		public Watch getWatch() {
			return this.watch;
		}

		public static class Watch {

			/**
			 * File watching.
			 */
			private final File file = new File();

			public File getFile() {
				return this.file;
			}

			public static class File {

				/**
				 * Quiet period, after which changes are detected.
				 */
				private Duration quietPeriod = Duration.ofSeconds(10);

				public Duration getQuietPeriod() {
					return this.quietPeriod;
				}

				public void setQuietPeriod(Duration quietPeriod) {
					this.quietPeriod = quietPeriod;
				}

			}

		}

	}

}
