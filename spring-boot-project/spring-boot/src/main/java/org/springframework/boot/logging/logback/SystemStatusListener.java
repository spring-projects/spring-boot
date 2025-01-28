/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.logging.logback;

import java.io.PrintStream;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.status.OnPrintStreamStatusListenerBase;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.status.StatusManager;

/**
 * {@link StatusListener} used to print appropriate status messages to {@link System#out}
 * or {@link System#err}.
 *
 * @author Dmytro Nosan
 * @author Phillip Webb
 */
final class SystemStatusListener extends OnPrintStreamStatusListenerBase {

	private final boolean debug;

	private SystemStatusListener(boolean debug) {
		this.debug = debug;
		setResetResistant(false);
		if (!this.debug) {
			setRetrospective(0);
		}
	}

	@Override
	public void addStatusEvent(Status status) {
		if (this.debug || status.getLevel() >= Status.WARN) {
			super.addStatusEvent(status);
		}
	}

	@Override
	protected PrintStream getPrintStream() {
		return (!this.debug) ? System.err : System.out;
	}

	static void addTo(LoggerContext loggerContext) {
		addTo(loggerContext, false);
	}

	static void addTo(LoggerContext loggerContext, boolean debug) {
		SystemStatusListener listener = new SystemStatusListener(debug);
		listener.setContext(loggerContext);
		StatusManager sm = loggerContext.getStatusManager();
		if (!sm.getCopyOfStatusListenerList().contains(listener) && sm.add(listener)) {
			listener.start();
		}
	}

	@Override
	public boolean equals(Object obj) {
		return (obj != null) && (obj.getClass() == getClass());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

}
