/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.sendgrid;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for SendGrid.
 *
 * @author Maciej Walkowiak
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.sendgrid")
public class SendGridProperties {

	/**
	 * SendGrid API key.
	 */
	private String apiKey;

	/**
	 * Proxy configuration.
	 */
	private Proxy proxy;

	/**
	 * Returns the API key.
	 * @return the API key
	 */
	public String getApiKey() {
		return this.apiKey;
	}

	/**
	 * Sets the API key for SendGrid.
	 * @param apiKey the API key to be set
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * Returns the proxy object associated with this SendGridProperties instance.
	 * @return the proxy object
	 */
	public Proxy getProxy() {
		return this.proxy;
	}

	/**
	 * Sets the proxy for the SendGridProperties.
	 * @param proxy the proxy to be set
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * Checks if a proxy is configured in the SendGridProperties.
	 * @return true if a proxy is configured, false otherwise
	 */
	public boolean isProxyConfigured() {
		return this.proxy != null && this.proxy.getHost() != null && this.proxy.getPort() != null;
	}

	/**
	 * Proxy class.
	 */
	public static class Proxy {

		/**
		 * SendGrid proxy host.
		 */
		private String host;

		/**
		 * SendGrid proxy port.
		 */
		private Integer port;

		/**
		 * Returns the host of the Proxy.
		 * @return the host of the Proxy
		 */
		public String getHost() {
			return this.host;
		}

		/**
		 * Sets the host for the Proxy.
		 * @param host the host to be set
		 */
		public void setHost(String host) {
			this.host = host;
		}

		/**
		 * Returns the port number of the Proxy.
		 * @return the port number of the Proxy
		 */
		public Integer getPort() {
			return this.port;
		}

		/**
		 * Sets the port number for the proxy.
		 * @param port the port number to be set
		 */
		public void setPort(Integer port) {
			this.port = port;
		}

	}

}
