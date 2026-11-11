/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

/**
 * Response from {@code docker compose version}.
 *
 * @param version the Docker Compose version
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
record DockerCliComposeVersionResponse(String version) {

}
