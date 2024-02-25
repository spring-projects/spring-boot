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

	/**
     * Constructs a new DockerHost object with the specified address.
     * 
     * @param address the address of the Docker host
     */
    public DockerHost(String address) {
		this(address, false, null);
	}

	/**
     * Constructs a new DockerHost object with the specified address, secure flag, and certificate path.
     * 
     * @param address the address of the Docker host
     * @param secure a boolean flag indicating whether the connection to the Docker host should be secure
     * @param certificatePath the path to the certificate file for secure connection
     */
    public DockerHost(String address, boolean secure, String certificatePath) {
		this.address = address;
		this.secure = secure;
		this.certificatePath = certificatePath;
	}

	/**
     * Returns the address of the Docker host.
     *
     * @return the address of the Docker host
     */
    public String getAddress() {
		return this.address;
	}

	/**
     * Returns a boolean value indicating whether the Docker host is secure.
     * 
     * @return true if the Docker host is secure, false otherwise
     */
    public boolean isSecure() {
		return this.secure;
	}

	/**
     * Returns the path of the certificate file used by the DockerHost.
     *
     * @return the path of the certificate file
     */
    public String getCertificatePath() {
		return this.certificatePath;
	}

}
