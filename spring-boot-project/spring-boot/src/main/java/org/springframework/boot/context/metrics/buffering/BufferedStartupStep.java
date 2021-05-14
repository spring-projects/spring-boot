/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.boot.context.metrics.buffering;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.core.metrics.StartupStep;
import org.springframework.util.Assert;

/**
 * {@link StartupStep} implementation to be buffered by
 * {@link BufferingApplicationStartup}. Its processing time is recorded using
 * {@link System#nanoTime()}.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
class BufferedStartupStep implements StartupStep {

	private final String name;

	private final long id;

	private final BufferedStartupStep parent;

	private final List<Tag> tags = new ArrayList<>();

	private final Consumer<BufferedStartupStep> recorder;

	private final Instant startTime;

	private final AtomicBoolean ended = new AtomicBoolean();

	BufferedStartupStep(BufferedStartupStep parent, String name, long id, Instant startTime,
			Consumer<BufferedStartupStep> recorder) {
		this.parent = parent;
		this.name = name;
		this.id = id;
		this.startTime = startTime;
		this.recorder = recorder;
	}

	BufferedStartupStep getParent() {
		return this.parent;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public long getId() {
		return this.id;
	}

	Instant getStartTime() {
		return this.startTime;
	}

	@Override
	public Long getParentId() {
		return (this.parent != null) ? this.parent.getId() : null;
	}

	@Override
	public Tags getTags() {
		return Collections.unmodifiableList(this.tags)::iterator;
	}

	@Override
	public StartupStep tag(String key, Supplier<String> value) {
		return this.tag(key, value.get());
	}

	@Override
	public StartupStep tag(String key, String value) {
		Assert.state(!this.ended.get(), "StartupStep has already ended.");
		this.tags.add(new DefaultTag(key, value));
		return this;
	}

	@Override
	public void end() {
		this.ended.set(true);
		this.recorder.accept(this);
	}

	boolean isEnded() {
		return this.ended.get();
	}

	static class DefaultTag implements Tag {

		private final String key;

		private final String value;

		DefaultTag(String key, String value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String getKey() {
			return this.key;
		}

		@Override
		public String getValue() {
			return this.value;
		}

	}

}
