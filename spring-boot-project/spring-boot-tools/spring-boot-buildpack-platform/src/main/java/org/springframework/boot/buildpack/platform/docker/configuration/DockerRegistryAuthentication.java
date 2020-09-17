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
 * Docker registry authentication configuration.
 *
 * @author Scott Frederick
 * @since 2.4.0
 */
public interface DockerRegistryAuthentication {

	/**
	 * Create the auth header that should be used for docker authentication.
	 * @return the auth header
	 */
	String createAuthHeader();

}
