/*
 * Copyright 2012-2024 the original author or authors.
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
