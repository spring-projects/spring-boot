/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

import java.util.Map;

/**
 * Response from {@link DockerCliCommand.ComposeConfig docker compose config}.
 *
 * @param name project name
 * @param services services
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
record DockerCliComposeConfigResponse(String name, Map<String, DockerCliComposeConfigResponse.Service> services) {

	/**
	 * Docker compose service.
	 *
	 * @param image the image
	 */
	record Service(String image) {

	}

}
