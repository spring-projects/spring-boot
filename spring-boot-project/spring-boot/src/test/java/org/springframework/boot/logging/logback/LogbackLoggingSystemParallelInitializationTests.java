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

package org.springframework.boot.logging.logback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.testsupport.classpath.ForkedClassPath;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for parallel initialization of {@link LogbackLoggingSystem} that are separate
 * from {@link LogbackLoggingSystemTests}. This isolation allows them to have complete
 * control over how and when the logging system is initialized.
 *
 * @author Andy Wilkinson
 */
class LogbackLoggingSystemParallelInitializationTests {

	private final LoggingSystem loggingSystem = LoggingSystem
		.get(LogbackLoggingSystemParallelInitializationTests.class.getClassLoader());

	@AfterEach
	void cleanUp() {
		this.loggingSystem.cleanUp();
		((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
	}

	@Test
	@ForkedClassPath
	void noExceptionsAreThrownWhenBeforeInitializeIsCalledInParallel() {
		List<Thread> threads = new ArrayList<>();
		List<Throwable> exceptions = new CopyOnWriteArrayList<>();
		for (int i = 0; i < 10; i++) {
			Thread thread = new Thread(() -> this.loggingSystem.beforeInitialize());
			thread.setUncaughtExceptionHandler((t, ex) -> exceptions.add(ex));
			threads.add(thread);
		}
		threads.forEach(Thread::start);
		threads.forEach(this::join);
		assertThat(exceptions).isEmpty();
	}

	private void join(Thread thread) {
		try {
			thread.join();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(ex);
		}
	}

}
