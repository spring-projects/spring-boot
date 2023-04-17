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

import org.springframework.boot.origin.Origin;

/**
 * An origin which points to a service defined in docker compose.
 *
 * @param composeFile docker compose file
 * @param serviceName name of the docker compose service
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public record DockerComposeOrigin(DockerComposeFile composeFile, String serviceName) implements Origin {

	@Override
	public String toString() {
		return "Docker compose service '%s' defined in '%s'".formatted(this.serviceName,
				(this.composeFile != null) ? this.composeFile : "default compose file");
	}

}
