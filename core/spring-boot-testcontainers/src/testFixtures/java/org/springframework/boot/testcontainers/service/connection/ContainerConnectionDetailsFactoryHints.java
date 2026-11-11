/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.service.connection;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory.ContainerConnectionDetailsFactoriesRuntimeHints;

public final class ContainerConnectionDetailsFactoryHints {

	private ContainerConnectionDetailsFactoryHints() {
	}

	public static RuntimeHints getRegisteredHints(ClassLoader classLoader) {
		RuntimeHints hints = new RuntimeHints();
		new ContainerConnectionDetailsFactoriesRuntimeHints().registerHints(hints, classLoader);
		return hints;
	}

}
