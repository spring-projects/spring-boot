/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.sendgrid;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for SendGrid.
 *
 * @author Maciej Walkowiak
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.sendgrid")
public class SendGridProperties {

	/**
	 * SendGrid username. Alternative to api key.
	 */
	private String username;

	/**
	 * SendGrid password.
	 */
	private String password;

	/**
	 * SendGrid api key. Alternative to username/password.
	 */
	private String apiKey;

	/**
	 * Proxy configuration.
	 */
	private Proxy proxy;

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getApiKey() {
		return this.apiKey;
	}

	public void setApiKey(final String apiKey) {
		this.apiKey = apiKey;
	}

	public Proxy getProxy() {
		return this.proxy;
	}

	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	public boolean isProxyConfigured() {
		return this.proxy != null && this.proxy.getHost() != null
				&& this.proxy.getPort() != null;
	}

	public static class Proxy {

		/**
		 * SendGrid proxy host.
		 */
		private String host;

		/**
		 * SendGrid proxy port.
		 */
		private Integer port;

		public String getHost() {
			return this.host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public Integer getPort() {
			return this.port;
		}

		public void setPort(Integer port) {
			this.port = port;
		}

	}

}
