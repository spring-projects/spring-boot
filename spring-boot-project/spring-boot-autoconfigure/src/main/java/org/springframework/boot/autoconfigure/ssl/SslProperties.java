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
