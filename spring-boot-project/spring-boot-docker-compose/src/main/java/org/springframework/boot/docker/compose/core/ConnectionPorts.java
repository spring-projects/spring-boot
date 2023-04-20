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

import java.util.List;

/**
 * Provides access to the ports that can be used to connect to a {@link RunningService}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 * @see RunningService
 */
public interface ConnectionPorts {

	/**
	 * Return the host port mapped to the given container port.
	 * @param containerPort the container port. This is usually the standard port for the
	 * service (e.g. port 80 for HTTP)
	 * @return the host port. This can be an ephemeral port that is different from the
	 * container port
	 * @throws IllegalStateException if the container port is not mapped
	 */
	int get(int containerPort);

	/**
	 * Return all host ports in use.
	 * @return a list of all host ports
	 * @see #getAll(String)
	 */
	List<Integer> getAll();

	/**
	 * Return all host ports in use that match the given protocol.
	 * @param protocol the protocol in use (for example 'tcp') or {@code null} to return
	 * all host ports
	 * @return a list of all host ports using the given protocol
	 */
	List<Integer> getAll(String protocol);

}
