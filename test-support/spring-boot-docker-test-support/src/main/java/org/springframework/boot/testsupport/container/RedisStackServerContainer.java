/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link GenericContainer} for Redis Stack Server.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
public class RedisStackServerContainer extends GenericContainer<RedisStackServerContainer> {

	public RedisStackServerContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
		addExposedPorts(6379);
	}

}
