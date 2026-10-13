/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.deployment;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Deployment tests for Tomcat.
 *
 * @author Christoph Dreis
 * @author Scott Frederick
 */
@Testcontainers(disabledWithoutDocker = true)
class TomcatDeploymentTests extends AbstractDeploymentTests {

	@Container
	static WarDeploymentContainer container = new WarDeploymentContainer("tomcat:11.0.10-jdk17",
			"/usr/local/tomcat/webapps", DEFAULT_PORT);

	@Override
	WarDeploymentContainer getContainer() {
		return container;
	}

}
