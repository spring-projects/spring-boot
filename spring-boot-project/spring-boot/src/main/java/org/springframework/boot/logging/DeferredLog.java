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

	private List<Line> lines = new ArrayList<>();

	@Override
	public boolean isTraceEnabled() {
		return true;
	}

	@Override
	public boolean isDebugEnabled() {
		return true;
	}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public boolean isFatalEnabled() {
		return true;
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
		this.lines.add(new Line(level, message, t));
	}

	public void replayTo(Class<?> destination) {
		replayTo(LogFactory.getLog(destination));
	}

	public void replayTo(Log destination) {
		for (Line line : this.lines) {
			line.replayTo(destination);
		}
		this.lines.clear();
	}

	public static Log replay(Log source, Class<?> destination) {
		return replay(source, LogFactory.getLog(destination));
	}

	public static Log replay(Log source, Log destination) {
		if (source instanceof DeferredLog) {
			((DeferredLog) source).replayTo(destination);
		}
		return destination;
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

		public void replayTo(Log log) {
			switch (this.level) {
			case TRACE:
				log.trace(this.message, this.throwable);
				return;
			case DEBUG:
				log.debug(this.message, this.throwable);
				return;
			case INFO:
				log.info(this.message, this.throwable);
				return;
			case WARN:
				log.warn(this.message, this.throwable);
				return;
			case ERROR:
				log.error(this.message, this.throwable);
				return;
			case FATAL:
				log.fatal(this.message, this.throwable);
				return;
			}
		}

	}

}
