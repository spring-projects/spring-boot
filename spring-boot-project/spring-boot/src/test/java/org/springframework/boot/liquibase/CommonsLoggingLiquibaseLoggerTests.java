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

package org.springframework.boot.liquibase;

import org.apache.commons.logging.Log;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CommonsLoggingLiquibaseLogger}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class CommonsLoggingLiquibaseLoggerTests {

	private Log delegate = mock(Log.class);

	private CommonsLoggingLiquibaseLogger logger;

	private Throwable ex = new Exception();

	@Before
	public void setup() {
		this.logger = new MockCommonsLoggingLiquibaseLogger(CommonsLoggingLiquibaseLoggerTests.this.delegate);
	}

	@Test
	public void debug() {
		given(this.delegate.isDebugEnabled()).willReturn(true);
		this.logger.debug("debug");
		verify(this.delegate).debug("debug");
	}

	@Test
	public void debugWithException() {
		given(this.delegate.isDebugEnabled()).willReturn(true);
		this.logger.debug("debug", this.ex);
		verify(this.delegate).debug("debug", this.ex);
	}

	@Test
	public void debugWithLoggerOff() {
		given(this.delegate.isDebugEnabled()).willReturn(false);
		this.logger.debug("debug");
		verify(this.delegate, never()).debug("debug");
	}

	@Test
	public void info() {
		given(this.delegate.isInfoEnabled()).willReturn(true);
		this.logger.info("info");
		verify(this.delegate).info("info");
	}

	@Test
	public void infoWithException() {
		given(this.delegate.isInfoEnabled()).willReturn(true);
		this.logger.info("info", this.ex);
		verify(this.delegate).info("info", this.ex);
	}

	@Test
	public void infoWithLoggerOff() {
		given(this.delegate.isInfoEnabled()).willReturn(false);
		this.logger.info("info");
		verify(this.delegate, never()).info("info");
	}

	@Test
	public void warning() {
		given(this.delegate.isWarnEnabled()).willReturn(true);
		this.logger.warning("warning");
		verify(this.delegate).warn("warning");
	}

	@Test
	public void warningWithException() {
		given(this.delegate.isWarnEnabled()).willReturn(true);
		this.logger.warning("warning", this.ex);
		verify(this.delegate).warn("warning", this.ex);
	}

	@Test
	public void warningWithLoggerOff() {
		given(this.delegate.isWarnEnabled()).willReturn(false);
		this.logger.warning("warning");
		verify(this.delegate, never()).warn("warning");
	}

	@Test
	public void severe() {
		given(this.delegate.isErrorEnabled()).willReturn(true);
		this.logger.severe("severe");
		verify(this.delegate).error("severe");
	}

	@Test
	public void severeWithException() {
		given(this.delegate.isErrorEnabled()).willReturn(true);
		this.logger.severe("severe", this.ex);
		verify(this.delegate).error("severe", this.ex);
	}

	@Test
	public void severeWithLoggerOff() {
		given(this.delegate.isErrorEnabled()).willReturn(false);
		this.logger.severe("severe");
		verify(this.delegate, never()).error("severe");
	}

	private class MockCommonsLoggingLiquibaseLogger
			extends CommonsLoggingLiquibaseLogger {
		MockCommonsLoggingLiquibaseLogger(Log logger) {
			super(logger);
		}

	}

}
