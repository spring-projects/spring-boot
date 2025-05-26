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

package org.springframework.boot.testcontainers.lifecycle;

import java.lang.reflect.InaccessibleObjectException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.lifecycle.Startables;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * JUnit extension used by reset startables.
 *
 * @author Phillip Webb
 */
class ResetStartablesExtension implements BeforeEachCallback, AfterEachCallback {

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		reset();
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		reset();
	}

	private void reset() {
		try {
			Object executor = ReflectionTestUtils.getField(Startables.class, "EXECUTOR");
			Object threadFactory = ReflectionTestUtils.getField(executor, "threadFactory");
			AtomicLong counter = (AtomicLong) ReflectionTestUtils.getField(threadFactory, "COUNTER");
			counter.set(0);
		}
		catch (InaccessibleObjectException ex) {
			throw new IllegalStateException(
					"Unable to reset field. Please run with '--add-opens=java.base/java.util.concurrent=ALL-UNNAMED'",
					ex);
		}
	}

}
