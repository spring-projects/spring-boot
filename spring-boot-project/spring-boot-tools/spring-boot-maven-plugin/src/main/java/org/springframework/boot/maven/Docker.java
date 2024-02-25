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

package org.springframework.boot.maven;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration;

/**
 * Docker configuration options.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 * @since 2.4.0
 */
public class Docker {

	private String host;

	private String context;

	private boolean tlsVerify;

	private String certPath;

	private boolean bindHostToBuilder;

	private DockerRegistry builderRegistry;

	private DockerRegistry publishRegistry;

	/**
	 * The host address of the Docker daemon.
	 * @return the Docker host
	 */
	public String getHost() {
		return this.host;
	}

	/**
     * Sets the host for the Docker class.
     * 
     * @param host the host to be set
     */
    void setHost(String host) {
		this.host = host;
	}

	/**
	 * The Docker context to use to retrieve host configuration.
	 * @return the Docker context
	 */
	public String getContext() {
		return this.context;
	}

	/**
     * Sets the context for the Docker class.
     * 
     * @param context the context to be set
     */
    public void setContext(String context) {
		this.context = context;
	}

	/**
	 * Whether the Docker daemon requires TLS communication.
	 * @return {@code true} to enable TLS
	 */
	public boolean isTlsVerify() {
		return this.tlsVerify;
	}

	/**
     * Sets the value of the tlsVerify property.
     * 
     * @param tlsVerify the new value for tlsVerify
     */
    void setTlsVerify(boolean tlsVerify) {
		this.tlsVerify = tlsVerify;
	}

	/**
	 * The path to TLS certificate and key files required for TLS communication with the
	 * Docker daemon.
	 * @return the TLS certificate path
	 */
	public String getCertPath() {
		return this.certPath;
	}

	/**
     * Sets the path of the certificate file.
     * 
     * @param certPath the path of the certificate file
     */
    void setCertPath(String certPath) {
		this.certPath = certPath;
	}

	/**
	 * Whether to use the configured Docker host in the builder container.
	 * @return {@code true} to use the configured Docker host in the builder container
	 */
	public boolean isBindHostToBuilder() {
		return this.bindHostToBuilder;
	}

	/**
     * Sets the value of the bindHostToBuilder property.
     * 
     * @param bindHostToBuilder the new value for the bindHostToBuilder property
     */
    void setBindHostToBuilder(boolean bindHostToBuilder) {
		this.bindHostToBuilder = bindHostToBuilder;
	}

	/**
	 * Configuration of the Docker registry where builder and run images are stored.
	 * @return the registry configuration
	 */
	DockerRegistry getBuilderRegistry() {
		return this.builderRegistry;
	}

	/**
	 * Sets the {@link DockerRegistry} that configures authentication to the builder
	 * registry.
	 * @param builderRegistry the registry configuration
	 */
	void setBuilderRegistry(DockerRegistry builderRegistry) {
		this.builderRegistry = builderRegistry;
	}

	/**
	 * Configuration of the Docker registry where the generated image will be published.
	 * @return the registry configuration
	 */
	DockerRegistry getPublishRegistry() {
		return this.publishRegistry;
	}

	/**
	 * Sets the {@link DockerRegistry} that configures authentication to the publishing
	 * registry.
	 * @param builderRegistry the registry configuration
	 */
	void setPublishRegistry(DockerRegistry builderRegistry) {
		this.publishRegistry = builderRegistry;
	}

	/**
	 * Returns this configuration as a {@link DockerConfiguration} instance. This method
	 * should only be called when the configuration is complete and will no longer be
	 * changed.
	 * @return the Docker configuration
	 */
	DockerConfiguration asDockerConfiguration() {
		DockerConfiguration dockerConfiguration = new DockerConfiguration();
		dockerConfiguration = customizeHost(dockerConfiguration);
		dockerConfiguration = dockerConfiguration.withBindHostToBuilder(this.bindHostToBuilder);
		dockerConfiguration = customizeBuilderAuthentication(dockerConfiguration);
		dockerConfiguration = customizePublishAuthentication(dockerConfiguration);
		return dockerConfiguration;
	}

	/**
     * Customizes the Docker host configuration.
     * 
     * @param dockerConfiguration the Docker configuration to be customized
     * @return the customized Docker configuration
     * @throws IllegalArgumentException if both context and host are provided
     */
    private DockerConfiguration customizeHost(DockerConfiguration dockerConfiguration) {
		if (this.context != null && this.host != null) {
			throw new IllegalArgumentException(
					"Invalid Docker configuration, either context or host can be provided but not both");
		}
		if (this.context != null) {
			return dockerConfiguration.withContext(this.context);
		}
		if (this.host != null) {
			return dockerConfiguration.withHost(this.host, this.tlsVerify, this.certPath);
		}
		return dockerConfiguration;
	}

	/**
     * Customizes the authentication configuration for the Docker builder registry.
     * 
     * @param dockerConfiguration the Docker configuration to customize
     * @return the customized Docker configuration
     * @throws IllegalArgumentException if the Docker builder registry configuration is invalid
     */
    private DockerConfiguration customizeBuilderAuthentication(DockerConfiguration dockerConfiguration) {
		if (this.builderRegistry == null || this.builderRegistry.isEmpty()) {
			return dockerConfiguration;
		}
		if (this.builderRegistry.hasTokenAuth() && !this.builderRegistry.hasUserAuth()) {
			return dockerConfiguration.withBuilderRegistryTokenAuthentication(this.builderRegistry.getToken());
		}
		if (this.builderRegistry.hasUserAuth() && !this.builderRegistry.hasTokenAuth()) {
			return dockerConfiguration.withBuilderRegistryUserAuthentication(this.builderRegistry.getUsername(),
					this.builderRegistry.getPassword(), this.builderRegistry.getUrl(), this.builderRegistry.getEmail());
		}
		throw new IllegalArgumentException(
				"Invalid Docker builder registry configuration, either token or username/password must be provided");
	}

	/**
     * Customizes the authentication configuration for publishing Docker images.
     * 
     * @param dockerConfiguration the original Docker configuration
     * @return the customized Docker configuration with the appropriate authentication settings
     * @throws IllegalArgumentException if the Docker publish registry configuration is invalid
     */
    private DockerConfiguration customizePublishAuthentication(DockerConfiguration dockerConfiguration) {
		if (this.publishRegistry == null || this.publishRegistry.isEmpty()) {
			return dockerConfiguration.withEmptyPublishRegistryAuthentication();
		}
		if (this.publishRegistry.hasTokenAuth() && !this.publishRegistry.hasUserAuth()) {
			return dockerConfiguration.withPublishRegistryTokenAuthentication(this.publishRegistry.getToken());
		}
		if (this.publishRegistry.hasUserAuth() && !this.publishRegistry.hasTokenAuth()) {
			return dockerConfiguration.withPublishRegistryUserAuthentication(this.publishRegistry.getUsername(),
					this.publishRegistry.getPassword(), this.publishRegistry.getUrl(), this.publishRegistry.getEmail());
		}
		throw new IllegalArgumentException(
				"Invalid Docker publish registry configuration, either token or username/password must be provided");
	}

	/**
	 * Encapsulates Docker registry authentication configuration options.
	 */
	public static class DockerRegistry {

		private String username;

		private String password;

		private String url;

		private String email;

		private String token;

		/**
         * Constructs a new DockerRegistry object.
         */
        public DockerRegistry() {
		}

		/**
         * Constructs a new DockerRegistry object with the specified username, password, URL, and email.
         * 
         * @param username the username to authenticate with the Docker registry
         * @param password the password to authenticate with the Docker registry
         * @param url the URL of the Docker registry
         * @param email the email associated with the Docker registry account
         */
        public DockerRegistry(String username, String password, String url, String email) {
			this.username = username;
			this.password = password;
			this.url = url;
			this.email = email;
		}

		/**
         * Constructs a new DockerRegistry object with the specified token.
         * 
         * @param token the token to authenticate with the Docker registry
         */
        public DockerRegistry(String token) {
			this.token = token;
		}

		/**
		 * The username that will be used for user authentication to the registry.
		 * @return the username
		 */
		public String getUsername() {
			return this.username;
		}

		/**
         * Sets the username for the DockerRegistry.
         * 
         * @param username the username to be set
         */
        void setUsername(String username) {
			this.username = username;
		}

		/**
		 * The password that will be used for user authentication to the registry.
		 * @return the password
		 */
		public String getPassword() {
			return this.password;
		}

		/**
         * Sets the password for the DockerRegistry.
         * 
         * @param password the password to be set
         */
        void setPassword(String password) {
			this.password = password;
		}

		/**
		 * The email address that will be used for user authentication to the registry.
		 * @return the email address
		 */
		public String getEmail() {
			return this.email;
		}

		/**
         * Sets the email for the DockerRegistry.
         * 
         * @param email the email to be set for the DockerRegistry
         */
        void setEmail(String email) {
			this.email = email;
		}

		/**
		 * The URL of the registry.
		 * @return the registry URL
		 */
		String getUrl() {
			return this.url;
		}

		/**
         * Sets the URL of the Docker registry.
         * 
         * @param url the URL of the Docker registry
         */
        void setUrl(String url) {
			this.url = url;
		}

		/**
		 * The token that will be used for token authentication to the registry.
		 * @return the authentication token
		 */
		public String getToken() {
			return this.token;
		}

		/**
         * Sets the token for the DockerRegistry.
         * 
         * @param token the token to be set
         */
        void setToken(String token) {
			this.token = token;
		}

		/**
         * Checks if the DockerRegistry object is empty.
         * 
         * @return true if the DockerRegistry object is empty, false otherwise
         */
        boolean isEmpty() {
			return this.username == null && this.password == null && this.url == null && this.email == null
					&& this.token == null;
		}

		/**
         * Checks if the DockerRegistry instance has a token authentication.
         * 
         * @return true if the DockerRegistry instance has a token authentication, false otherwise.
         */
        boolean hasTokenAuth() {
			return this.token != null;
		}

		/**
         * Checks if the user has authentication credentials.
         * 
         * @return true if the user has authentication credentials, false otherwise
         */
        boolean hasUserAuth() {
			return this.username != null && this.password != null;
		}

	}

}
