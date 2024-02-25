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

package org.springframework.boot.buildpack.platform.docker.configuration;

import org.springframework.util.Assert;

/**
 * Docker configuration options.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 * @since 2.4.0
 */
public final class DockerConfiguration {

	private final DockerHostConfiguration host;

	private final DockerRegistryAuthentication builderAuthentication;

	private final DockerRegistryAuthentication publishAuthentication;

	private final boolean bindHostToBuilder;

	/**
	 * Constructs a new DockerConfiguration object with default values.
	 */
	public DockerConfiguration() {
		this(null, null, null, false);
	}

	/**
	 * Constructs a new DockerConfiguration object with the specified parameters.
	 * @param host the DockerHostConfiguration object representing the configuration for
	 * the Docker host
	 * @param builderAuthentication the DockerRegistryAuthentication object representing
	 * the authentication configuration for the builder
	 * @param publishAuthentication the DockerRegistryAuthentication object representing
	 * the authentication configuration for publishing
	 * @param bindHostToBuilder a boolean indicating whether to bind the host to the
	 * builder
	 */
	private DockerConfiguration(DockerHostConfiguration host, DockerRegistryAuthentication builderAuthentication,
			DockerRegistryAuthentication publishAuthentication, boolean bindHostToBuilder) {
		this.host = host;
		this.builderAuthentication = builderAuthentication;
		this.publishAuthentication = publishAuthentication;
		this.bindHostToBuilder = bindHostToBuilder;
	}

	/**
	 * Returns the Docker host configuration.
	 * @return the Docker host configuration
	 */
	public DockerHostConfiguration getHost() {
		return this.host;
	}

	/**
	 * Returns the value of the bindHostToBuilder property.
	 * @return true if the bindHostToBuilder property is set to true, false otherwise.
	 */
	public boolean isBindHostToBuilder() {
		return this.bindHostToBuilder;
	}

	/**
	 * Returns the DockerRegistryAuthentication object used for builder authentication.
	 * @return the DockerRegistryAuthentication object used for builder authentication
	 */
	public DockerRegistryAuthentication getBuilderRegistryAuthentication() {
		return this.builderAuthentication;
	}

	/**
	 * Returns the authentication details for the publish registry.
	 * @return the authentication details for the publish registry
	 */
	public DockerRegistryAuthentication getPublishRegistryAuthentication() {
		return this.publishAuthentication;
	}

	/**
	 * Sets the host configuration for the Docker connection.
	 * @param address the address of the Docker host
	 * @param secure true if the connection should be secure, false otherwise
	 * @param certificatePath the path to the certificate file for secure connection
	 * @return the updated DockerConfiguration object
	 * @throws IllegalArgumentException if the address is null
	 */
	public DockerConfiguration withHost(String address, boolean secure, String certificatePath) {
		Assert.notNull(address, "Address must not be null");
		return new DockerConfiguration(DockerHostConfiguration.forAddress(address, secure, certificatePath),
				this.builderAuthentication, this.publishAuthentication, this.bindHostToBuilder);
	}

	/**
	 * Sets the context for the Docker configuration.
	 * @param context the context to set
	 * @return the updated Docker configuration
	 * @throws IllegalArgumentException if the context is null
	 */
	public DockerConfiguration withContext(String context) {
		Assert.notNull(context, "Context must not be null");
		return new DockerConfiguration(DockerHostConfiguration.forContext(context), this.builderAuthentication,
				this.publishAuthentication, this.bindHostToBuilder);
	}

	/**
	 * Sets whether to bind the host to the builder in the Docker configuration.
	 * @param bindHostToBuilder true to bind the host to the builder, false otherwise
	 * @return a new DockerConfiguration object with the updated bindHostToBuilder value
	 */
	public DockerConfiguration withBindHostToBuilder(boolean bindHostToBuilder) {
		return new DockerConfiguration(this.host, this.builderAuthentication, this.publishAuthentication,
				bindHostToBuilder);
	}

	/**
	 * Sets the authentication token for the builder registry.
	 * @param token the authentication token to be set
	 * @return a new DockerConfiguration object with the updated builder registry
	 * authentication
	 * @throws IllegalArgumentException if the token is null
	 */
	public DockerConfiguration withBuilderRegistryTokenAuthentication(String token) {
		Assert.notNull(token, "Token must not be null");
		return new DockerConfiguration(this.host, new DockerRegistryTokenAuthentication(token),
				this.publishAuthentication, this.bindHostToBuilder);
	}

	/**
	 * Sets the builder registry user authentication for this Docker configuration.
	 * @param username the username for the builder registry user authentication (must not
	 * be null)
	 * @param password the password for the builder registry user authentication (must not
	 * be null)
	 * @param url the URL for the builder registry user authentication
	 * @param email the email for the builder registry user authentication
	 * @return a new DockerConfiguration object with the specified builder registry user
	 * authentication
	 * @throws IllegalArgumentException if the username or password is null
	 */
	public DockerConfiguration withBuilderRegistryUserAuthentication(String username, String password, String url,
			String email) {
		Assert.notNull(username, "Username must not be null");
		Assert.notNull(password, "Password must not be null");
		return new DockerConfiguration(this.host, new DockerRegistryUserAuthentication(username, password, url, email),
				this.publishAuthentication, this.bindHostToBuilder);
	}

	/**
	 * Sets the authentication token for publishing to a Docker registry.
	 * @param token the authentication token to be set
	 * @return a new DockerConfiguration object with the updated authentication token
	 * @throws IllegalArgumentException if the token is null
	 */
	public DockerConfiguration withPublishRegistryTokenAuthentication(String token) {
		Assert.notNull(token, "Token must not be null");
		return new DockerConfiguration(this.host, this.builderAuthentication,
				new DockerRegistryTokenAuthentication(token), this.bindHostToBuilder);
	}

	/**
	 * Sets the authentication credentials for publishing to a Docker registry.
	 * @param username the username for authentication
	 * @param password the password for authentication
	 * @param url the URL of the Docker registry
	 * @param email the email associated with the Docker registry account
	 * @return a new DockerConfiguration object with the updated authentication
	 * credentials
	 * @throws IllegalArgumentException if any of the parameters are null
	 */
	public DockerConfiguration withPublishRegistryUserAuthentication(String username, String password, String url,
			String email) {
		Assert.notNull(username, "Username must not be null");
		Assert.notNull(password, "Password must not be null");
		return new DockerConfiguration(this.host, this.builderAuthentication,
				new DockerRegistryUserAuthentication(username, password, url, email), this.bindHostToBuilder);
	}

	/**
	 * Creates a new DockerConfiguration object with an empty publish registry
	 * authentication.
	 * @return a new DockerConfiguration object with an empty publish registry
	 * authentication
	 */
	public DockerConfiguration withEmptyPublishRegistryAuthentication() {
		return new DockerConfiguration(this.host, this.builderAuthentication,
				new DockerRegistryUserAuthentication("", "", "", ""), this.bindHostToBuilder);
	}

	/**
	 * DockerHostConfiguration class.
	 */
	public static class DockerHostConfiguration {

		private final String address;

		private final String context;

		private final boolean secure;

		private final String certificatePath;

		/**
		 * Constructs a new DockerHostConfiguration with the specified address, context,
		 * secure flag, and certificate path.
		 * @param address the address of the Docker host
		 * @param context the context path of the Docker host
		 * @param secure a flag indicating whether the connection to the Docker host is
		 * secure
		 * @param certificatePath the path to the certificate file for secure connection
		 */
		public DockerHostConfiguration(String address, String context, boolean secure, String certificatePath) {
			this.address = address;
			this.context = context;
			this.secure = secure;
			this.certificatePath = certificatePath;
		}

		/**
		 * Returns the address of the Docker host.
		 * @return the address of the Docker host
		 */
		public String getAddress() {
			return this.address;
		}

		/**
		 * Returns the context of the DockerHostConfiguration.
		 * @return the context of the DockerHostConfiguration
		 */
		public String getContext() {
			return this.context;
		}

		/**
		 * Returns a boolean value indicating whether the Docker host configuration is
		 * secure.
		 * @return true if the Docker host configuration is secure, false otherwise
		 */
		public boolean isSecure() {
			return this.secure;
		}

		/**
		 * Returns the path of the certificate file used for Docker host configuration.
		 * @return the path of the certificate file
		 */
		public String getCertificatePath() {
			return this.certificatePath;
		}

		/**
		 * Creates a DockerHostConfiguration object for the specified address.
		 * @param address the address of the Docker host
		 * @return a DockerHostConfiguration object with the specified address
		 */
		public static DockerHostConfiguration forAddress(String address) {
			return new DockerHostConfiguration(address, null, false, null);
		}

		/**
		 * Creates a DockerHostConfiguration object for the specified address.
		 * @param address the address of the Docker host
		 * @param secure true if the connection to the Docker host should be secure, false
		 * otherwise
		 * @param certificatePath the path to the certificate file for secure connection
		 * (can be null if secure is false)
		 * @return a DockerHostConfiguration object for the specified address
		 */
		public static DockerHostConfiguration forAddress(String address, boolean secure, String certificatePath) {
			return new DockerHostConfiguration(address, null, secure, certificatePath);
		}

		/**
		 * Creates a DockerHostConfiguration object for the specified context.
		 * @param context the context for which the DockerHostConfiguration is created
		 * @return a DockerHostConfiguration object with the specified context
		 */
		static DockerHostConfiguration forContext(String context) {
			return new DockerHostConfiguration(null, context, false, null);
		}

	}

}
