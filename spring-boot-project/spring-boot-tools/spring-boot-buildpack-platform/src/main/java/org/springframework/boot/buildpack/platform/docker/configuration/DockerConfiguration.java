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

	private final DockerRegistryAuthentication authentication;

	public DockerConfiguration() {
		this(null, null);
	}

	private DockerConfiguration(DockerHost host, DockerRegistryAuthentication authentication) {
		this.host = host;
		this.authentication = authentication;
	}

	public DockerHost getHost() {
		return this.host;
	}

	public DockerRegistryAuthentication getRegistryAuthentication() {
		return this.authentication;
	}

	public DockerConfiguration withHost(String address, boolean secure, String certificatePath) {
		Assert.notNull(address, "Address must not be null");
		return new DockerConfiguration(new DockerHost(address, secure, certificatePath), this.authentication);
	}

	public DockerConfiguration withRegistryTokenAuthentication(String token) {
		Assert.notNull(token, "Token must not be null");
		return new DockerConfiguration(this.host, new DockerRegistryTokenAuthentication(token));
	}

	public DockerConfiguration withRegistryUserAuthentication(String username, String password, String url,
			String email) {
		Assert.notNull(username, "Username must not be null");
		Assert.notNull(password, "Password must not be null");
		return new DockerConfiguration(this.host, new DockerRegistryUserAuthentication(username, password, url, email));
	}

}
