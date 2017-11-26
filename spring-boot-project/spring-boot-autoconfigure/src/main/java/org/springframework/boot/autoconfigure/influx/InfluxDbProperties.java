/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.influx;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.core.io.Resource;

/**
 * Configuration properties for InfluxDB.
 *
 * @author Sergey Kuptsov
 * @author Stephane Nicoll
 * @author Ali Dehghani
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.influx")
public class InfluxDbProperties {

	/**
	 * URL of the InfluxDB instance to which to connect.
	 */
	private String url;

	/**
	 * Login user.
	 */
	private String user;

	/**
	 * Login password.
	 */
	private String password;

	/**
	 * Encapsulates SSL configurations for to-be-configured InfluxDB client. If
	 * {@code null}, then the communication would be performed over plain HTTP.
	 */
	@NestedConfigurationProperty
	private Ssl ssl;

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	/**
	 * Encapsulates SSL configurations for the InfluxDB client.
	 */
	public static class Ssl {

		/**
		 * Determines SSL communication is enabled or not.
		 */
		private boolean enabled;

		/**
		 * File containing the X.509 certificate. The file content is usually a Base64
		 * encoded DER certificate, enclosed between "-----BEGIN CERTIFICATE-----" and
		 * "-----END CERTIFICATE-----".
		 */
		private Resource certificate;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Resource getCertificate() {
			return this.certificate;
		}

		public void setCertificate(Resource certificate) {
			this.certificate = certificate;
		}
	}
}
