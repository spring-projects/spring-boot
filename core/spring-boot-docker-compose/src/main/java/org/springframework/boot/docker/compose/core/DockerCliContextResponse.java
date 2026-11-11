/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

import org.jspecify.annotations.Nullable;

/**
 * Response from {@link DockerCliCommand.Context docker context}.
 *
 * @param name the name of the context
 * @param current if the context is the current one
 * @param dockerEndpoint the endpoint of the docker daemon, or {@code null} if not known
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
record DockerCliContextResponse(String name, boolean current, @Nullable String dockerEndpoint) {

}
