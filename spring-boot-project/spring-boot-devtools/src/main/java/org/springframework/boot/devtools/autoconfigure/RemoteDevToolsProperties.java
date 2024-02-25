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

package org.springframework.boot.devtools.autoconfigure;

/**
 * Configuration properties for remote Spring Boot applications.
 *
 * @author Phillip Webb
 * @author Rob Winch
 * @since 1.3.0
 * @see DevToolsProperties
 */
public class RemoteDevToolsProperties {

	public static final String DEFAULT_CONTEXT_PATH = "/.~~spring-boot!~";

	public static final String DEFAULT_SECRET_HEADER_NAME = "X-AUTH-TOKEN";

	/**
	 * Context path used to handle the remote connection.
	 */
	private String contextPath = DEFAULT_CONTEXT_PATH;

	/**
	 * A shared secret required to establish a connection (required to enable remote
	 * support).
	 */
	private String secret;

	/**
	 * HTTP header used to transfer the shared secret.
	 */
	private String secretHeaderName = DEFAULT_SECRET_HEADER_NAME;

	private final Restart restart = new Restart();

	private final Proxy proxy = new Proxy();

	/**
     * Returns the context path of the RemoteDevToolsProperties.
     *
     * @return the context path of the RemoteDevToolsProperties
     */
    public String getContextPath() {
		return this.contextPath;
	}

	/**
     * Sets the context path for the RemoteDevToolsProperties.
     * 
     * @param contextPath the context path to be set
     */
    public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	/**
     * Returns the secret value.
     *
     * @return the secret value
     */
    public String getSecret() {
		return this.secret;
	}

	/**
     * Sets the secret for the RemoteDevToolsProperties.
     * 
     * @param secret the secret to be set
     */
    public void setSecret(String secret) {
		this.secret = secret;
	}

	/**
     * Returns the secret header name.
     *
     * @return the secret header name
     */
    public String getSecretHeaderName() {
		return this.secretHeaderName;
	}

	/**
     * Sets the name of the secret header.
     * 
     * @param secretHeaderName the name of the secret header
     */
    public void setSecretHeaderName(String secretHeaderName) {
		this.secretHeaderName = secretHeaderName;
	}

	/**
     * Returns the restart object associated with this RemoteDevToolsProperties instance.
     *
     * @return the restart object
     */
    public Restart getRestart() {
		return this.restart;
	}

	/**
     * Returns the proxy object associated with this RemoteDevToolsProperties instance.
     *
     * @return the proxy object
     */
    public Proxy getProxy() {
		return this.proxy;
	}

	/**
     * Restart class.
     */
    public static class Restart {

		/**
		 * Whether to enable remote restart.
		 */
		private boolean enabled = true;

		/**
         * Returns the current status of the enabled flag.
         *
         * @return true if the enabled flag is set, false otherwise.
         */
        public boolean isEnabled() {
			return this.enabled;
		}

		/**
         * Sets the enabled status of the Restart object.
         * 
         * @param enabled the new enabled status to be set
         */
        public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	/**
     * Proxy class.
     */
    public static class Proxy {

		/**
		 * The host of the proxy to use to connect to the remote application.
		 */
		private String host;

		/**
		 * The port of the proxy to use to connect to the remote application.
		 */
		private Integer port;

		/**
         * Returns the host of the Proxy.
         *
         * @return the host of the Proxy
         */
        public String getHost() {
			return this.host;
		}

		/**
         * Sets the host for the Proxy.
         * 
         * @param host the host to be set
         */
        public void setHost(String host) {
			this.host = host;
		}

		/**
         * Returns the port number of the Proxy.
         *
         * @return the port number of the Proxy
         */
        public Integer getPort() {
			return this.port;
		}

		/**
         * Sets the port number for the proxy.
         * 
         * @param port the port number to be set
         */
        public void setPort(Integer port) {
			this.port = port;
		}

	}

}
