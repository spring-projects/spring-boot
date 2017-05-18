/*
 * Copyright 2012-2016 the original author or authors.
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
	public void isTraceEnabled() throws Exception {
		assertThat(this.deferredLog.isTraceEnabled()).isTrue();
	}

	@Test
	public void isDebugEnabled() throws Exception {
		assertThat(this.deferredLog.isDebugEnabled()).isTrue();
	}

	@Test
	public void isInfoEnabled() throws Exception {
		assertThat(this.deferredLog.isInfoEnabled()).isTrue();
	}

	@Test
	public void isWarnEnabled() throws Exception {
		assertThat(this.deferredLog.isWarnEnabled()).isTrue();
	}

	@Test
	public void isErrorEnabled() throws Exception {
		assertThat(this.deferredLog.isErrorEnabled()).isTrue();
	}

	@Test
	public void isFatalEnabled() throws Exception {
		assertThat(this.deferredLog.isFatalEnabled()).isTrue();
	}

	@Test
	public void trace() throws Exception {
		this.deferredLog.trace(this.message);
		this.deferredLog.replayTo(this.log);
		verify(this.log).trace(this.message, null);
	}

	@Test
	public void traceWithThrowable() throws Exception {
		this.deferredLog.trace(this.message, this.throwable);
		this.deferredLog.replayTo(this.log);
		verify(this.log).trace(this.message, this.throwable);
	}

	@Test
	public void debug() throws Exception {
		this.deferredLog.debug(this.message);
		this.deferredLog.replayTo(this.log);
		verify(this.log).debug(this.message, null);
	}

	@Test
	public void debugWithThrowable() throws Exception {
		this.deferredLog.debug(this.message, this.throwable);
		this.deferredLog.replayTo(this.log);
		verify(this.log).debug(this.message, this.throwable);
	}

	@Test
	public void info() throws Exception {
		this.deferredLog.info(this.message);
		this.deferredLog.replayTo(this.log);
		verify(this.log).info(this.message, null);
	}

	@Test
	public void infoWithThrowable() throws Exception {
		this.deferredLog.info(this.message, this.throwable);
		this.deferredLog.replayTo(this.log);
		verify(this.log).info(this.message, this.throwable);
	}

	@Test
	public void warn() throws Exception {
		this.deferredLog.warn(this.message);
		this.deferredLog.replayTo(this.log);
		verify(this.log).warn(this.message, null);
	}

	@Test
	public void warnWithThrowable() throws Exception {
		this.deferredLog.warn(this.message, this.throwable);
		this.deferredLog.replayTo(this.log);
		verify(this.log).warn(this.message, this.throwable);
	}

	@Test
	public void error() throws Exception {
		this.deferredLog.error(this.message);
		this.deferredLog.replayTo(this.log);
		verify(this.log).error(this.message, null);
	}

	@Test
	public void errorWithThrowable() throws Exception {
		this.deferredLog.error(this.message, this.throwable);
		this.deferredLog.replayTo(this.log);
		verify(this.log).error(this.message, this.throwable);
	}

	@Test
	public void fatal() throws Exception {
		this.deferredLog.fatal(this.message);
		this.deferredLog.replayTo(this.log);
		verify(this.log).fatal(this.message, null);
	}

	@Test
	public void fatalWithThrowable() throws Exception {
		this.deferredLog.fatal(this.message, this.throwable);
		this.deferredLog.replayTo(this.log);
		verify(this.log).fatal(this.message, this.throwable);
	}

	@Test
	public void clearsOnReplayTo() throws Exception {
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
