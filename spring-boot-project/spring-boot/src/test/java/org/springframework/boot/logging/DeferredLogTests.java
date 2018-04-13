/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.logging;

import org.apache.commons.logging.Log;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link DeferredLog}.
 *
 * @author Phillip Webb
 */
public class DeferredLogTests {

	private DeferredLog deferredLog = new DeferredLog();

	private Object message = "Message";

	private Throwable throwable = new IllegalStateException();

	private Log log = mock(Log.class);

	@Test
	public void isTraceEnabled() {
		assertThat(this.deferredLog.isTraceEnabled()).isTrue();
	}

	@Test
	public void isDebugEnabled() {
		assertThat(this.deferredLog.isDebugEnabled()).isTrue();
	}

	@Test
	public void isInfoEnabled() {
		assertThat(this.deferredLog.isInfoEnabled()).isTrue();
	}

	@Test
	public void isWarnEnabled() {
		assertThat(this.deferredLog.isWarnEnabled()).isTrue();
	}

	@Test
	public void isErrorEnabled() {
		assertThat(this.deferredLog.isErrorEnabled()).isTrue();
	}

	@Test
	public void isFatalEnabled() {
		assertThat(this.deferredLog.isFatalEnabled()).isTrue();
	}

	@Test
	public void trace() {
		this.deferredLog.trace(this.message);
		this.deferredLog.replayTo(this.log);
		verify(this.log).trace(this.message, null);
	}

	@Test
	public void traceWithThrowable() {
		this.deferredLog.trace(this.message, this.throwable);
		this.deferredLog.replayTo(this.log);
		verify(this.log).trace(this.message, this.throwable);
	}

	@Test
	public void debug() {
		this.deferredLog.debug(this.message);
		this.deferredLog.replayTo(this.log);
		verify(this.log).debug(this.message, null);
	}

	@Test
	public void debugWithThrowable() {
		this.deferredLog.debug(this.message, this.throwable);
		this.deferredLog.replayTo(this.log);
		verify(this.log).debug(this.message, this.throwable);
	}

	@Test
	public void info() {
		this.deferredLog.info(this.message);
		this.deferredLog.replayTo(this.log);
		verify(this.log).info(this.message, null);
	}

	@Test
	public void infoWithThrowable() {
		this.deferredLog.info(this.message, this.throwable);
		this.deferredLog.replayTo(this.log);
		verify(this.log).info(this.message, this.throwable);
	}

	@Test
	public void warn() {
		this.deferredLog.warn(this.message);
		this.deferredLog.replayTo(this.log);
		verify(this.log).warn(this.message, null);
	}

	@Test
	public void warnWithThrowable() {
		this.deferredLog.warn(this.message, this.throwable);
		this.deferredLog.replayTo(this.log);
		verify(this.log).warn(this.message, this.throwable);
	}

	@Test
	public void error() {
		this.deferredLog.error(this.message);
		this.deferredLog.replayTo(this.log);
		verify(this.log).error(this.message, null);
	}

	@Test
	public void errorWithThrowable() {
		this.deferredLog.error(this.message, this.throwable);
		this.deferredLog.replayTo(this.log);
		verify(this.log).error(this.message, this.throwable);
	}

	@Test
	public void fatal() {
		this.deferredLog.fatal(this.message);
		this.deferredLog.replayTo(this.log);
		verify(this.log).fatal(this.message, null);
	}

	@Test
	public void fatalWithThrowable() {
		this.deferredLog.fatal(this.message, this.throwable);
		this.deferredLog.replayTo(this.log);
		verify(this.log).fatal(this.message, this.throwable);
	}

	@Test
	public void clearsOnReplayTo() {
		this.deferredLog.info("1");
		this.deferredLog.fatal("2");
		Log log2 = mock(Log.class);
		this.deferredLog.replayTo(this.log);
		this.deferredLog.replayTo(log2);
		verify(this.log).info("1", null);
		verify(this.log).fatal("2", null);
		verifyNoMoreInteractions(this.log);
		verifyZeroInteractions(log2);
	}

}
