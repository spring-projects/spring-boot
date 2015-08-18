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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Marker;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;

/**
 * {@link Appender} that can remap {@link ILoggingEvent} {@link Level}s as they are
 * written.
 *
 * @author Phillip Webb
 * @see #setRemapLevels(String)
 * @see #setDestinationLogger(String)
 */
public class LevelRemappingAppender extends AppenderBase<ILoggingEvent> {

	private static final Map<Level, Level> DEFAULT_REMAPS = Collections.singletonMap(
			Level.INFO, Level.DEBUG);

	private String destinationLogger = Logger.ROOT_LOGGER_NAME;

	private Map<Level, Level> remapLevels = DEFAULT_REMAPS;

	/**
	 * Create a new {@link LevelRemappingAppender}.
	 */
	public LevelRemappingAppender() {
	}

	/**
	 * Create a new {@link LevelRemappingAppender} with a specific destination logger.
	 * @param destinationLogger the destination logger
	 */
	public LevelRemappingAppender(String destinationLogger) {
		this.destinationLogger = destinationLogger;
	}

	@Override
	protected void append(ILoggingEvent event) {
		AppendableLogger logger = getLogger(this.destinationLogger);
		Level remapped = this.remapLevels.get(event.getLevel());
		logger.callAppenders(remapped == null ? event : new RemappedLoggingEvent(event));
	}

	protected AppendableLogger getLogger(String name) {
		LoggerContext loggerContext = (LoggerContext) this.context;
		return new AppendableLogger(loggerContext.getLogger(name));
	}

	/**
	 * Sets the destination logger that will be used to send remapped events. If not
	 * specified the root logger is used.
	 * @param destinationLogger the destinationLogger name
	 */
	public void setDestinationLogger(String destinationLogger) {
		Assert.hasLength(destinationLogger, "DestinationLogger must not be empty");
		this.destinationLogger = destinationLogger;
	}

	/**
	 * Set the remapped level.
	 * @param remapLevels Comma separated String of remapped levels in the form
	 * {@literal "FROM->TO"}. For example, {@literal "DEBUG->TRACE,ERROR->WARN"}.
	 */
	public void setRemapLevels(String remapLevels) {
		Assert.hasLength(remapLevels, "RemapLevels must not be empty");
		this.remapLevels = new HashMap<Level, Level>();
		for (String remap : StringUtils.commaDelimitedListToStringArray(remapLevels)) {
			String[] split = StringUtils.split(remap, "->");
			Assert.notNull(split, "Remap element '" + remap + "' must contain '->'");
			this.remapLevels.put(Level.toLevel(split[0]), Level.toLevel(split[1]));
		}
	}

	/**
	 * Simple wrapper around a logger that can have events appended.
	 */
	protected static class AppendableLogger {

		private Logger logger;

		public AppendableLogger(Logger logger) {
			this.logger = logger;
		}

		public void callAppenders(ILoggingEvent event) {
			if (this.logger.isEnabledFor(event.getLevel())) {
				this.logger.callAppenders(event);
			}
		}
	}

	/**
	 * Decorate an existing {@link ILoggingEvent} changing the level to DEBUG.
	 */
	private class RemappedLoggingEvent implements ILoggingEvent {

		private final ILoggingEvent event;

		public RemappedLoggingEvent(ILoggingEvent event) {
			this.event = event;
		}

		@Override
		public String getThreadName() {
			return this.event.getThreadName();
		}

		@Override
		public Level getLevel() {
			Level remappedLevel = LevelRemappingAppender.this.remapLevels.get(this.event
					.getLevel());
			return (remappedLevel == null ? this.event.getLevel() : remappedLevel);
		}

		@Override
		public String getMessage() {
			return this.event.getMessage();
		}

		@Override
		public Object[] getArgumentArray() {
			return this.event.getArgumentArray();
		}

		@Override
		public String getFormattedMessage() {
			return this.event.getFormattedMessage();
		}

		@Override
		public String getLoggerName() {
			return this.event.getLoggerName();
		}

		@Override
		public LoggerContextVO getLoggerContextVO() {
			return this.event.getLoggerContextVO();
		}

		@Override
		public IThrowableProxy getThrowableProxy() {
			return this.event.getThrowableProxy();
		}

		@Override
		public StackTraceElement[] getCallerData() {
			return this.event.getCallerData();
		}

		@Override
		public boolean hasCallerData() {
			return this.event.hasCallerData();
		}

		@Override
		public Marker getMarker() {
			return this.event.getMarker();
		}

		@Override
		public Map<String, String> getMDCPropertyMap() {
			return this.event.getMDCPropertyMap();
		}

		@Override
		@Deprecated
		public Map<String, String> getMdc() {
			return this.event.getMdc();
		}

		@Override
		public long getTimeStamp() {
			return this.event.getTimeStamp();
		}

		@Override
		public void prepareForDeferredProcessing() {
			this.event.prepareForDeferredProcessing();
		}

	}

}
