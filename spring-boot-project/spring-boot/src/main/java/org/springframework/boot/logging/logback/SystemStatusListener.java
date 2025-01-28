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
import java.util.List;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.BasicStatusManager;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.util.StatusPrinter2;

/**
 * {@link StatusListener} used to print appropriate status messages to {@link System#out}
 * or {@link System#err}. Note that this class extends {@link OnConsoleStatusListener} so
 * that {@link BasicStatusManager#add(StatusListener)} does not add the same listener
 * twice. It also implement a version of retrospectivePrint that can filter status
 * messages by level.
 *
 * @author Dmytro Nosan
 * @author Phillip Webb
 */
final class SystemStatusListener extends OnConsoleStatusListener {

	static final long RETROSPECTIVE_THRESHOLD = 300;

	private static final StatusPrinter2 PRINTER = new StatusPrinter2();

	private final boolean debug;

	private SystemStatusListener(boolean debug) {
		this.debug = debug;
		setResetResistant(false);
		setRetrospective(0);
	}

	@Override
	public void start() {
		super.start();
		retrospectivePrint();
	}

	private void retrospectivePrint() {
		if (this.context == null) {
			return;
		}
		long now = System.currentTimeMillis();
		List<Status> statusList = this.context.getStatusManager().getCopyOfStatusList();
		statusList.stream().filter((status) -> isPrintable(status, now)).forEach(this::print);
	}

	private void print(Status status) {
		StringBuilder sb = new StringBuilder();
		PRINTER.buildStr(sb, "", status);
		getPrintStream().print(sb);
	}

	@Override
	public void addStatusEvent(Status status) {
		if (isPrintable(status, 0)) {
			super.addStatusEvent(status);
		}
	}

	private boolean isPrintable(Status status, long now) {
		boolean timstampInRange = (now == 0 || (now - status.getTimestamp()) < RETROSPECTIVE_THRESHOLD);
		return timstampInRange && (this.debug || status.getLevel() >= Status.WARN);
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
		StatusManager statusManager = loggerContext.getStatusManager();
		if (statusManager.add(listener)) {
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
