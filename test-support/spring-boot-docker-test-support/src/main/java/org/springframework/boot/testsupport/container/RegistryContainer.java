/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link GenericContainer} for Docker Registry.
 *
 * @author Phillip Webb
 */
public class RegistryContainer extends GenericContainer<RegistryContainer> {

	public RegistryContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
		addExposedPorts(5000);
		addEnv("SERVER_NAME", "localhost");
	}

}
