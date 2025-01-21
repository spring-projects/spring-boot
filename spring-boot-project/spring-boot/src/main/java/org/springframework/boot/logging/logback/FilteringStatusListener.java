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

import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;

/**
 * Logback {@link StatusListener} that filters {@link Status} by its logging level and
 * delegates to the underlying {@code StatusListener}.
 *
 * @author Dmytro Nosan
 */
class FilteringStatusListener extends ContextAwareBase implements StatusListener, LifeCycle {

	private final StatusListener delegate;

	private final int levelThreshold;

	/**
	 * Creates a new {@link FilteringStatusListener}.
	 * @param delegate the {@link StatusListener} to delegate to
	 * @param levelThreshold the minimum log level accepted for delegation
	 */
	FilteringStatusListener(StatusListener delegate, int levelThreshold) {
		this.delegate = delegate;
		this.levelThreshold = levelThreshold;
	}

	@Override
	public void addStatusEvent(Status status) {
		if (status.getLevel() >= this.levelThreshold) {
			this.delegate.addStatusEvent(status);
		}
	}

	@Override
	public boolean isResetResistant() {
		return this.delegate.isResetResistant();
	}

	@Override
	public void start() {
		if (this.delegate instanceof LifeCycle lifeCycle) {
			lifeCycle.start();
		}
	}

	@Override
	public void stop() {
		if (this.delegate instanceof LifeCycle lifeCycle) {
			lifeCycle.stop();
		}
	}

	@Override
	public boolean isStarted() {
		if (this.delegate instanceof LifeCycle lifeCycle) {
			return lifeCycle.isStarted();
		}
		return true;
	}

}
