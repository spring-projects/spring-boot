/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.deployment;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Deployment tests for Open Liberty.
 *
 * @author Christoph Dreis
 * @author Scott Frederick
 */
@Testcontainers(disabledWithoutDocker = true)
class OpenLibertyDeploymentTests extends AbstractDeploymentTests {

	private static final int PORT = 9080;

	@Container
	static WarDeploymentContainer container = new WarDeploymentContainer(
			"icr.io/appcafe/open-liberty:full-java17-openj9-ubi", "/config/dropins", PORT,
			(builder) -> builder.run("sed -i 's/javaee-8.0/jakartaee-10.0/g' /config/server.xml"));

	@Override
	WarDeploymentContainer getContainer() {
		return container;
	}

	@Override
	protected int getPort() {
		return PORT;
	}

}
