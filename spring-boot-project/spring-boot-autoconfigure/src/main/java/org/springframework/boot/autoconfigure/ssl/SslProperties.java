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
@ConfigurationProperties(prefix = "spring.ssl")
public class SslProperties {

	/**
	 * SSL bundles.
	 */
	private final Bundles bundle = new Bundles();

	/**
	 * Returns the bundle associated with this SslProperties object.
	 * @return the bundle associated with this SslProperties object
	 */
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

		/**
		 * Returns the PEM SSL bundle properties.
		 * @return a map containing the PEM SSL bundle properties, where the key is a
		 * string representing the bundle name and the value is an object of type
		 * PemSslBundleProperties
		 */
		public Map<String, PemSslBundleProperties> getPem() {
			return this.pem;
		}

		/**
		 * Returns the map of JKS SSL bundle properties.
		 * @return the map of JKS SSL bundle properties
		 */
		public Map<String, JksSslBundleProperties> getJks() {
			return this.jks;
		}

		/**
		 * Returns the watch object associated with this Bundles object.
		 * @return the watch object
		 */
		public Watch getWatch() {
			return this.watch;
		}

		/**
		 * Watch class.
		 */
		public static class Watch {

			/**
			 * File watching.
			 */
			private final File file = new File();

			/**
			 * Returns the file associated with this Watch object.
			 * @return the file associated with this Watch object
			 */
			public File getFile() {
				return this.file;
			}

			/**
			 * File class.
			 */
			public static class File {

				/**
				 * Quiet period, after which changes are detected.
				 */
				private Duration quietPeriod = Duration.ofSeconds(10);

				/**
				 * Returns the quiet period of the File.
				 * @return the quiet period of the File
				 */
				public Duration getQuietPeriod() {
					return this.quietPeriod;
				}

				/**
				 * Sets the quiet period for the File.
				 * @param quietPeriod the duration of the quiet period
				 */
				public void setQuietPeriod(Duration quietPeriod) {
					this.quietPeriod = quietPeriod;
				}

			}

		}

	}

}
