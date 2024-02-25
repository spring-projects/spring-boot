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

	private Log destination;

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

	/**
     * Returns a boolean value indicating whether tracing is enabled.
     * 
     * @return {@code true} if tracing is enabled, {@code false} otherwise.
     */
    @Override
	public boolean isTraceEnabled() {
		synchronized (this.lines) {
			return (this.destination == null) || this.destination.isTraceEnabled();
		}
	}

	/**
     * Returns a boolean value indicating whether debug mode is enabled.
     * 
     * @return true if debug mode is enabled, false otherwise
     */
    @Override
	public boolean isDebugEnabled() {
		synchronized (this.lines) {
			return (this.destination == null) || this.destination.isDebugEnabled();
		}
	}

	/**
     * Returns a boolean value indicating whether the info level logging is enabled.
     * 
     * @return {@code true} if the info level logging is enabled, {@code false} otherwise.
     */
    @Override
	public boolean isInfoEnabled() {
		synchronized (this.lines) {
			return (this.destination == null) || this.destination.isInfoEnabled();
		}
	}

	/**
     * Returns a boolean value indicating whether the warn level logging is enabled.
     * 
     * @return {@code true} if the warn level logging is enabled, {@code false} otherwise.
     */
    @Override
	public boolean isWarnEnabled() {
		synchronized (this.lines) {
			return (this.destination == null) || this.destination.isWarnEnabled();
		}
	}

	/**
     * Returns a boolean value indicating whether error logging is enabled.
     * 
     * @return {@code true} if error logging is enabled, {@code false} otherwise.
     */
    @Override
	public boolean isErrorEnabled() {
		synchronized (this.lines) {
			return (this.destination == null) || this.destination.isErrorEnabled();
		}
	}

	/**
     * Returns a boolean value indicating whether the fatal level logging is enabled.
     * 
     * @return {@code true} if the fatal level logging is enabled, {@code false} otherwise.
     */
    @Override
	public boolean isFatalEnabled() {
		synchronized (this.lines) {
			return (this.destination == null) || this.destination.isFatalEnabled();
		}
	}

	/**
     * Logs a trace message.
     * 
     * @param message the message to be logged
     */
    @Override
	public void trace(Object message) {
		log(LogLevel.TRACE, message, null);
	}

	/**
     * Logs a trace level message with an optional throwable.
     * 
     * @param message the message to be logged
     * @param t the throwable to be logged (optional)
     */
    @Override
	public void trace(Object message, Throwable t) {
		log(LogLevel.TRACE, message, t);
	}

	/**
     * Logs a debug message.
     * 
     * @param message the message to be logged
     */
    @Override
	public void debug(Object message) {
		log(LogLevel.DEBUG, message, null);
	}

	/**
     * Logs a debug message with an optional throwable.
     * 
     * @param message the debug message to be logged
     * @param t the throwable to be logged (optional)
     */
    @Override
	public void debug(Object message, Throwable t) {
		log(LogLevel.DEBUG, message, t);
	}

	/**
     * Logs an informational message.
     * 
     * @param message the message to be logged
     */
    @Override
	public void info(Object message) {
		log(LogLevel.INFO, message, null);
	}

	/**
     * Logs an informational message with an optional throwable.
     *
     * @param message the message to be logged
     * @param t the throwable to be logged (optional)
     */
    @Override
	public void info(Object message, Throwable t) {
		log(LogLevel.INFO, message, t);
	}

	/**
     * Logs a warning message.
     * 
     * @param message the warning message to be logged
     */
    @Override
	public void warn(Object message) {
		log(LogLevel.WARN, message, null);
	}

	/**
     * Logs a warning message with an associated throwable.
     * 
     * @param message the warning message to be logged
     * @param t the throwable associated with the warning message
     */
    @Override
	public void warn(Object message, Throwable t) {
		log(LogLevel.WARN, message, t);
	}

	/**
     * Logs an error message.
     * 
     * @param message the error message to be logged
     */
    @Override
	public void error(Object message) {
		log(LogLevel.ERROR, message, null);
	}

	/**
     * Logs an error message with the specified throwable.
     * 
     * @param message the error message to be logged
     * @param t the throwable associated with the error
     */
    @Override
	public void error(Object message, Throwable t) {
		log(LogLevel.ERROR, message, t);
	}

	/**
     * Logs a fatal message.
     * 
     * @param message the message to be logged
     */
    @Override
	public void fatal(Object message) {
		log(LogLevel.FATAL, message, null);
	}

	/**
     * Logs a fatal message with an associated throwable.
     * 
     * @param message the message to be logged
     * @param t the throwable associated with the message
     */
    @Override
	public void fatal(Object message, Throwable t) {
		log(LogLevel.FATAL, message, t);
	}

	/**
     * Logs a message with the specified log level and optional throwable.
     * 
     * @param level   the log level of the message
     * @param message the message to be logged
     * @param t       the optional throwable associated with the message
     */
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

	/**
     * Switches over the destination of the DeferredLog.
     * <p>
     * This method is used to switch the destination of the DeferredLog by obtaining a new destination from the destinationSupplier.
     * The destination is updated in a synchronized block to ensure thread safety.
     * </p>
     */
    void switchOver() {
		synchronized (this.lines) {
			this.destination = this.destinationSupplier.get();
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
		if (source instanceof DeferredLog deferredLog) {
			deferredLog.replayTo(destination);
		}
		return destination;
	}

	/**
     * Logs a message with the specified log level and optional throwable.
     * 
     * @param log       the log instance to use for logging
     * @param level     the log level to use
     * @param message   the message to be logged
     * @param throwable the optional throwable to be logged
     */
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

	/**
     * Lines class.
     */
    static class Lines implements Iterable<Line> {

		private final List<Line> lines = new ArrayList<>();

		/**
         * Adds a new line to the lines list with the specified destination supplier, log level, message, and throwable.
         * 
         * @param destinationSupplier the supplier that provides the destination for the log
         * @param level the log level of the line
         * @param message the log message
         * @param throwable the throwable associated with the log, or null if none
         */
        void add(Supplier<Log> destinationSupplier, LogLevel level, Object message, Throwable throwable) {
			this.lines.add(new Line(destinationSupplier, level, message, throwable));
		}

		/**
         * Clears all the lines in the Lines object.
         */
        void clear() {
			this.lines.clear();
		}

		/**
         * Returns an iterator over the elements in this Lines object in proper sequence.
         *
         * @return an iterator over the elements in this Lines object in proper sequence
         */
        @Override
		public Iterator<Line> iterator() {
			return this.lines.iterator();
		}

	}

	/**
     * Line class.
     */
    static class Line {

		private final Supplier<Log> destinationSupplier;

		private final LogLevel level;

		private final Object message;

		private final Throwable throwable;

		/**
         * Constructs a new Line object with the specified destination supplier, log level, message, and throwable.
         * 
         * @param destinationSupplier the supplier of the log destination
         * @param level the log level
         * @param message the log message
         * @param throwable the throwable associated with the log message
         */
        Line(Supplier<Log> destinationSupplier, LogLevel level, Object message, Throwable throwable) {
			this.destinationSupplier = destinationSupplier;
			this.level = level;
			this.message = message;
			this.throwable = throwable;
		}

		/**
         * Returns the destination of the Line.
         *
         * @return the destination of the Line
         */
        Log getDestination() {
			return this.destinationSupplier.get();
		}

		/**
         * Returns the log level of the Line object.
         *
         * @return the log level of the Line object
         */
        LogLevel getLevel() {
			return this.level;
		}

		/**
         * Returns the message associated with this Line object.
         *
         * @return the message associated with this Line object
         */
        Object getMessage() {
			return this.message;
		}

		/**
         * Returns the throwable object associated with this Line.
         *
         * @return the throwable object associated with this Line
         */
        Throwable getThrowable() {
			return this.throwable;
		}

	}

}
