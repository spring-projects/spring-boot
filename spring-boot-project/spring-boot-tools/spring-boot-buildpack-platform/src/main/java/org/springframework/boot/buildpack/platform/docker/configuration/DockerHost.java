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

/**
 * Docker host connection options.
 *
 * @author Scott Frederick
 * @since 2.4.0
 */
public class DockerHost {

	private final String address;

	private final boolean secure;

	private final String certificatePath;

	public DockerHost(String address, boolean secure, String certificatePath) {
		this.address = address;
		this.secure = secure;
		this.certificatePath = certificatePath;
	}

	public String getAddress() {
		return this.address;
	}

	public boolean isSecure() {
		return this.secure;
	}

	public String getCertificatePath() {
		return this.certificatePath;
	}

}
