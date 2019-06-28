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

package org.springframework.boot.logging;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Deferred {@link Log} that can be used to store messages that shouldn't be written until
 * the logging system is fully initialized.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class DeferredLog implements Log {

	private volatile Log destination;

	private final List<Line> lines = new ArrayList<>();

	@Override
	public boolean isTraceEnabled() {
		synchronized (this.lines) {
			return (this.destination != null) ? this.destination.isTraceEnabled() : true;
		}
	}

	@Override
	public boolean isDebugEnabled() {
		synchronized (this.lines) {
			return (this.destination != null) ? this.destination.isDebugEnabled() : true;
		}
	}

	@Override
	public boolean isInfoEnabled() {
		synchronized (this.lines) {
			return (this.destination != null) ? this.destination.isInfoEnabled() : true;
		}
	}

	@Override
	public boolean isWarnEnabled() {
		synchronized (this.lines) {
			return (this.destination != null) ? this.destination.isWarnEnabled() : true;
		}
	}

	@Override
	public boolean isErrorEnabled() {
		synchronized (this.lines) {
			return (this.destination != null) ? this.destination.isErrorEnabled() : true;
		}
	}

	@Override
	public boolean isFatalEnabled() {
		synchronized (this.lines) {
			return (this.destination != null) ? this.destination.isFatalEnabled() : true;
		}
	}

	@Override
	public void trace(Object message) {
		log(LogLevel.TRACE, message, null);
	}

	@Override
	public void trace(Object message, Throwable t) {
		log(LogLevel.TRACE, message, t);
	}

	@Override
	public void debug(Object message) {
		log(LogLevel.DEBUG, message, null);
	}

	@Override
	public void debug(Object message, Throwable t) {
		log(LogLevel.DEBUG, message, t);
	}

	@Override
	public void info(Object message) {
		log(LogLevel.INFO, message, null);
	}

	@Override
	public void info(Object message, Throwable t) {
		log(LogLevel.INFO, message, t);
	}

	@Override
	public void warn(Object message) {
		log(LogLevel.WARN, message, null);
	}

	@Override
	public void warn(Object message, Throwable t) {
		log(LogLevel.WARN, message, t);
	}

	@Override
	public void error(Object message) {
		log(LogLevel.ERROR, message, null);
	}

	@Override
	public void error(Object message, Throwable t) {
		log(LogLevel.ERROR, message, t);
	}

	@Override
	public void fatal(Object message) {
		log(LogLevel.FATAL, message, null);
	}

	@Override
	public void fatal(Object message, Throwable t) {
		log(LogLevel.FATAL, message, t);
	}

	private void log(LogLevel level, Object message, Throwable t) {
		synchronized (this.lines) {
			if (this.destination != null) {
				logTo(this.destination, level, message, t);
			}
			else {
				this.lines.add(new Line(level, message, t));
			}
		}
	}

	/**
	 * Switch from deferred logging to immediate logging to the specified destination.
	 * @param destination the new log destination
	 * @since 2.1.0
	 */
	public void switchTo(Class<?> destination) {
		switchTo(LogFactory.getLog(destination));
	}

	/**
	 * Switch from deferred logging to immediate logging to the specified destination.
	 * @param destination the new log destination
	 * @since 2.1.0
	 */
	public void switchTo(Log destination) {
		synchronized (this.lines) {
			replayTo(destination);
			this.destination = destination;
		}
	}

	/**
	 * Replay deferred logging to the specified destination.
	 * @param destination the destination for the deferred log messages
	 */
	public void replayTo(Class<?> destination) {
		replayTo(LogFactory.getLog(destination));
	}

	/**
	 * Replay deferred logging to the specified destination.
	 * @param destination the destination for the deferred log messages
	 */
	public void replayTo(Log destination) {
		synchronized (this.lines) {
			for (Line line : this.lines) {
				logTo(destination, line.getLevel(), line.getMessage(), line.getThrowable());
			}
			this.lines.clear();
		}
	}

	/**
	 * Replay from a source log to a destination log when the source is deferred.
	 * @param source the source logger
	 * @param destination the destination logger class
	 * @return the destination
	 */
	public static Log replay(Log source, Class<?> destination) {
		return replay(source, LogFactory.getLog(destination));
	}

	/**
	 * Replay from a source log to a destination log when the source is deferred.
	 * @param source the source logger
	 * @param destination the destination logger
	 * @return the destination
	 */
	public static Log replay(Log source, Log destination) {
		if (source instanceof DeferredLog) {
			((DeferredLog) source).replayTo(destination);
		}
		return destination;
	}

	private static void logTo(Log log, LogLevel level, Object message, Throwable throwable) {
		switch (level) {
		case TRACE:
			log.trace(message, throwable);
			return;
		case DEBUG:
			log.debug(message, throwable);
			return;
		case INFO:
			log.info(message, throwable);
			return;
		case WARN:
			log.warn(message, throwable);
			return;
		case ERROR:
			log.error(message, throwable);
			return;
		case FATAL:
			log.fatal(message, throwable);
			return;
		}
	}

	private static class Line {

		private final LogLevel level;

		private final Object message;

		private final Throwable throwable;

		Line(LogLevel level, Object message, Throwable throwable) {
			this.level = level;
			this.message = message;
			this.throwable = throwable;
		}

		public LogLevel getLevel() {
			return this.level;
		}

		public Object getMessage() {
			return this.message;
		}

		public Throwable getThrowable() {
			return this.throwable;
		}

	}

}
