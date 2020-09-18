/*
 * Copyright 2012-2020 the original author or authors.
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

	private final DockerHost host;

	private final DockerRegistryAuthentication builderAuthentication;

	private final DockerRegistryAuthentication publishAuthentication;

	public DockerConfiguration() {
		this(null, null, null);
	}

	private DockerConfiguration(DockerHost host, DockerRegistryAuthentication builderAuthentication,
			DockerRegistryAuthentication publishAuthentication) {
		this.host = host;
		this.builderAuthentication = builderAuthentication;
		this.publishAuthentication = publishAuthentication;
	}

	public DockerHost getHost() {
		return this.host;
	}

	public DockerRegistryAuthentication getBuilderRegistryAuthentication() {
		return this.builderAuthentication;
	}

	public DockerRegistryAuthentication getPublishRegistryAuthentication() {
		return this.publishAuthentication;
	}

	public DockerConfiguration withHost(String address, boolean secure, String certificatePath) {
		Assert.notNull(address, "Address must not be null");
		return new DockerConfiguration(new DockerHost(address, secure, certificatePath), this.builderAuthentication,
				this.publishAuthentication);
	}

	public DockerConfiguration withBuilderRegistryTokenAuthentication(String token) {
		Assert.notNull(token, "Token must not be null");
		return new DockerConfiguration(this.host, new DockerRegistryTokenAuthentication(token),
				this.publishAuthentication);
	}

	public DockerConfiguration withBuilderRegistryUserAuthentication(String username, String password, String url,
			String email) {
		Assert.notNull(username, "Username must not be null");
		Assert.notNull(password, "Password must not be null");
		return new DockerConfiguration(this.host, new DockerRegistryUserAuthentication(username, password, url, email),
				this.publishAuthentication);
	}

	public DockerConfiguration withPublishRegistryTokenAuthentication(String token) {
		Assert.notNull(token, "Token must not be null");
		return new DockerConfiguration(this.host, this.builderAuthentication,
				new DockerRegistryTokenAuthentication(token));
	}

	public DockerConfiguration withPublishRegistryUserAuthentication(String username, String password, String url,
			String email) {
		Assert.notNull(username, "Username must not be null");
		Assert.notNull(password, "Password must not be null");
		return new DockerConfiguration(this.host, this.builderAuthentication,
				new DockerRegistryUserAuthentication(username, password, url, email));
	}

}
