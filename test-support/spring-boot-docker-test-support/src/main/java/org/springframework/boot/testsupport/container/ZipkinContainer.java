/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link GenericContainer} for Zipkin.
 *
 * @author Phillip Webb
 */
public class ZipkinContainer extends GenericContainer<ZipkinContainer> {

	public ZipkinContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
		withExposedPorts(9411);
	}

}
