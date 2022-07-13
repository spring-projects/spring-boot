/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * Deferred {@link Log} that can be used to store messages that shouldn't be written until
 * the logging system is fully initialized.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class DeferredLog implements Log {

	private volatile Log destination;

	private final Supplier<Log> destinationSupplier;

	private final Lines lines;

	/**
	 * Create a new {@link DeferredLog} instance.
	 */
	public DeferredLog() {
		this.destinationSupplier = null;
		this.lines = new Lines();
	}

	/**
	 * Create a new {@link DeferredLog} instance managed by a {@link DeferredLogFactory}.
	 * @param destination the switch-over destination
	 * @param lines the lines backing all related deferred logs
	 * @since 2.4.0
	 */
	DeferredLog(Supplier<Log> destination, Lines lines) {
		Assert.notNull(destination, "Destination must not be null");
		this.destinationSupplier = destination;
		this.lines = lines;
	}

	@Override
	public boolean isTraceEnabled() {
		synchronized (this.lines) {
			return (this.destination == null) || this.destination.isTraceEnabled();
		}
	}

	@Override
	public boolean isDebugEnabled() {
		synchronized (this.lines) {
			return (this.destination == null) || this.destination.isDebugEnabled();
		}
	}

	@Override
	public boolean isInfoEnabled() {
		synchronized (this.lines) {
			return (this.destination == null) || this.destination.isInfoEnabled();
		}
	}

	@Override
	public boolean isWarnEnabled() {
		synchronized (this.lines) {
			return (this.destination == null) || this.destination.isWarnEnabled();
		}
	}

	@Override
	public boolean isErrorEnabled() {
		synchronized (this.lines) {
			return (this.destination == null) || this.destination.isErrorEnabled();
		}
	}

	@Override
	public boolean isFatalEnabled() {
		synchronized (this.lines) {
			return (this.destination == null) || this.destination.isFatalEnabled();
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
				this.lines.add(this.destinationSupplier, level, message, t);
			}
		}
	}

	void switchOver() {
		this.destination = this.destinationSupplier.get();
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
		if (source instanceof DeferredLog deferredLog) {
			deferredLog.replayTo(destination);
		}
		return destination;
	}

	static void logTo(Log log, LogLevel level, Object message, Throwable throwable) {
		switch (level) {
			case TRACE -> log.trace(message, throwable);
			case DEBUG -> log.debug(message, throwable);
			case INFO -> log.info(message, throwable);
			case WARN -> log.warn(message, throwable);
			case ERROR -> log.error(message, throwable);
			case FATAL -> log.fatal(message, throwable);
		}
	}

	static class Lines implements Iterable<Line> {

		private final List<Line> lines = new ArrayList<>();

		void add(Supplier<Log> destinationSupplier, LogLevel level, Object message, Throwable throwable) {
			this.lines.add(new Line(destinationSupplier, level, message, throwable));
		}

		void clear() {
			this.lines.clear();
		}

		@Override
		public Iterator<Line> iterator() {
			return this.lines.iterator();
		}

	}

	static class Line {

		private final Supplier<Log> destinationSupplier;

		private final LogLevel level;

		private final Object message;

		private final Throwable throwable;

		Line(Supplier<Log> destinationSupplier, LogLevel level, Object message, Throwable throwable) {
			this.destinationSupplier = destinationSupplier;
			this.level = level;
			this.message = message;
			this.throwable = throwable;
		}

		Log getDestination() {
			return this.destinationSupplier.get();
		}

		LogLevel getLevel() {
			return this.level;
		}

		Object getMessage() {
			return this.message;
		}

		Throwable getThrowable() {
			return this.throwable;
		}

	}

}
