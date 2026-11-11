/*
 * Copyright 2012-present the original author or authors.
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

	private final LoggingSystem loggingSystem = LoggingSystem.get(getClass().getClassLoader());

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
			Thread thread = new Thread(this.loggingSystem::beforeInitialize);
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
