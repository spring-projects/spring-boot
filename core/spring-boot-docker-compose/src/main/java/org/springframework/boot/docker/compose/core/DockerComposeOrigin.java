/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.origin.Origin;

/**
 * An origin which points to a service defined in Docker Compose.
 *
 * @param composeFile the Docker Compose file
 * @param serviceName name of the Docker Compose service
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public record DockerComposeOrigin(@Nullable DockerComposeFile composeFile, String serviceName) implements Origin {

	@Override
	public String toString() {
		return "Docker compose service '%s' defined in %s".formatted(this.serviceName,
				(this.composeFile != null) ? this.composeFile : "default compose file");
	}

}
