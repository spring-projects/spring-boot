/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

import org.jspecify.annotations.Nullable;

/**
 * Response from {@link DockerCliCommand.ComposePs docker compose ps}.
 *
 * @param id the container ID
 * @param name the name of the service
 * @param image the image reference
 * @param state the state of the container
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
record DockerCliComposePsResponse(String id, String name, @Nullable String image, String state) {

}
