/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.testsupport.testcontainers;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

/**
 * {@link TestRule} for working with an optional Docker environment. Spins up a
 * {@link GenericContainer} if a valid docker environment is found.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class Container implements TestRule {

	private final int port;

	private final Supplier<GenericContainer<?>> containerFactory;

	private GenericContainer<?> container;

	<T extends GenericContainer<T>> Container(String dockerImageName, int port) {
		this(dockerImageName, port, null);
	}

	@SuppressWarnings({ "unchecked", "resource" })
	<T extends GenericContainer<T>> Container(String dockerImageName, int port,
			Consumer<T> customizer) {
		this.port = port;
		this.containerFactory = () -> {
			T container = (T) new GenericContainer<>(dockerImageName)
					.withExposedPorts(port);
			if (customizer != null) {
				customizer.accept(container);
			}
			return container;
		};
	}

	@Override
	public Statement apply(Statement base, Description description) {
		try {
			DockerClientFactory.instance().client();
		}
		catch (Throwable ex) {
			return new SkipStatement();
		}
		this.container = this.containerFactory.get();
		return this.container.apply(base, description);
	}

	public int getMappedPort() {
		return this.container.getMappedPort(this.port);
	}

	private static class SkipStatement extends Statement {

		@Override
		public void evaluate() {
			throw new AssumptionViolatedException(
					"Could not find a valid Docker environment.");
		}

	}

}
