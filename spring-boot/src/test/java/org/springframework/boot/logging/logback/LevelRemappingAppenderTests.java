/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.logging.logback;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.logging.logback.LevelRemappingAppender.AppendableLogger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LevelRemappingAppender}.
 * 
 * @author Phillip Webb
 */
public class LevelRemappingAppenderTests {

	private TestableLevelRemappingAppender appender;

	@Mock
	private AppendableLogger logger;

	@Captor
	private ArgumentCaptor<ILoggingEvent> logCaptor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.appender = spy(new TestableLevelRemappingAppender());
	}

	@Test
	public void useRootLoggerIfNoDestination() throws Exception {
		this.appender.append(mockLogEvent(Level.INFO));
		verify(this.appender).getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
	}

	@Test
	public void useSpecificDestination() throws Exception {
		this.appender.setDestinationLogger("org.mine");
		this.appender.append(mockLogEvent(Level.INFO));
		verify(this.appender).getLogger("org.mine");
	}

	@Test
	public void defaltRemapsInfo() throws Exception {
		this.appender.append(mockLogEvent(Level.INFO));
		verify(this.logger).callAppenders(this.logCaptor.capture());
		assertThat(this.logCaptor.getValue().getLevel(), equalTo(Level.DEBUG));
	}

	@Test
	public void customRemaps() throws Exception {
		this.appender.setRemapLevels("DEBUG->TRACE,ERROR->WARN");
		this.appender.append(mockLogEvent(Level.DEBUG));
		this.appender.append(mockLogEvent(Level.ERROR));
		verify(this.logger, times(2)).callAppenders(this.logCaptor.capture());
		assertThat(this.logCaptor.getAllValues().get(0).getLevel(), equalTo(Level.TRACE));
		assertThat(this.logCaptor.getAllValues().get(1).getLevel(), equalTo(Level.WARN));
	}

	private ILoggingEvent mockLogEvent(Level level) {
		ILoggingEvent event = mock(ILoggingEvent.class);
		given(event.getLevel()).willReturn(level);
		return event;
	}

	private class TestableLevelRemappingAppender extends LevelRemappingAppender {

		@Override
		protected AppendableLogger getLogger(String name) {
			return LevelRemappingAppenderTests.this.logger;
		}

	}
}
