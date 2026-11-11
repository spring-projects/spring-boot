/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

import java.io.IOException;

/**
 * Exception thrown by {@link ProcessRunner} when a processes will not start.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ProcessStartException extends RuntimeException {

	ProcessStartException(String[] command, IOException ex) {
		super("Unable to start command %s".formatted(String.join(" ", command)), ex);
	}

}
