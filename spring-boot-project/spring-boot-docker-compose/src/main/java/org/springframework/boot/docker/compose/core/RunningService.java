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

package org.springframework.boot.docker.compose.core;

import java.util.Map;

/**
 * Provides details of a running docker compose service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public interface RunningService {

	/**
	 * Return the name of the service.
	 * @return the service name
	 */
	String name();

	/**
	 * Return the image being used by the service.
	 * @return the service image
	 */
	ImageReference image();

	/**
	 * Return the host that can be used to connect to the service.
	 * @return the service host
	 */
	String host();

	/**
	 * Return the ports that can be used to connect to the service.
	 * @return the service ports
	 */
	ConnectionPorts ports();

	/**
	 * Return the environment defined for the service.
	 * @return the service env
	 */
	Map<String, String> env();

	/**
	 * Return the labels attached to the service.
	 * @return the service labels
	 */
	Map<String, String> labels();

}
