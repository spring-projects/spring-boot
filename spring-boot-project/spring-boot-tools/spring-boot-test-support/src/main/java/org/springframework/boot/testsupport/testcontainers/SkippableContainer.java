/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.testsupport.testcontainers;

import java.util.function.Supplier;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.FailureDetectingExternalResource;
import org.testcontainers.containers.GenericContainer;

/**
 * A {@link GenericContainer} decorator that skips test execution when Docker is not
 * available.
 *
 * @param <T> type of the underlying container
 * @author Andy Wilkinson
 */
public class SkippableContainer<T> implements TestRule {

	private final Supplier<T> containerFactory;

	private T container;

	public SkippableContainer(Supplier<T> containerFactory) {
		this.containerFactory = containerFactory;
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
		return ((FailureDetectingExternalResource) this.container).apply(base,
				description);
	}

	public T getContainer() {
		if (this.container == null) {
			throw new IllegalStateException(
					"Container cannot be accessed prior to test invocation");
		}
		return this.container;
	}

	private static class SkipStatement extends Statement {

		@Override
		public void evaluate() {
			throw new AssumptionViolatedException(
					"Could not find a valid Docker environment.");
		}

	}

}
