/*
 * Copyright 2002-2022 the original author or authors.
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

	/**
     * Constructs a new BufferedStartupStep with the specified parent, name, id, start time, and recorder.
     * 
     * @param parent the parent BufferedStartupStep of this step
     * @param name the name of this step
     * @param id the unique identifier of this step
     * @param startTime the start time of this step
     * @param recorder the consumer function used to record this step
     */
    BufferedStartupStep(BufferedStartupStep parent, String name, long id, Instant startTime,
			Consumer<BufferedStartupStep> recorder) {
		this.parent = parent;
		this.name = name;
		this.id = id;
		this.startTime = startTime;
		this.recorder = recorder;
	}

	/**
     * Returns the parent BufferedStartupStep of this BufferedStartupStep.
     *
     * @return the parent BufferedStartupStep of this BufferedStartupStep
     */
    BufferedStartupStep getParent() {
		return this.parent;
	}

	/**
     * Returns the name of the BufferedStartupStep.
     *
     * @return the name of the BufferedStartupStep
     */
    @Override
	public String getName() {
		return this.name;
	}

	/**
     * Returns the ID of the BufferedStartupStep.
     *
     * @return the ID of the BufferedStartupStep
     */
    @Override
	public long getId() {
		return this.id;
	}

	/**
     * Returns the start time of the BufferedStartupStep.
     *
     * @return the start time of the BufferedStartupStep
     */
    Instant getStartTime() {
		return this.startTime;
	}

	/**
     * Returns the ID of the parent of this BufferedStartupStep.
     * 
     * @return the ID of the parent, or null if there is no parent
     */
    @Override
	public Long getParentId() {
		return (this.parent != null) ? this.parent.getId() : null;
	}

	/**
     * Returns the tags associated with this BufferedStartupStep.
     * 
     * @return the tags associated with this BufferedStartupStep
     */
    @Override
	public Tags getTags() {
		return Collections.unmodifiableList(this.tags)::iterator;
	}

	/**
     * Adds a tag with the specified key and value to this startup step.
     * 
     * @param key the key of the tag
     * @param value the value of the tag
     * @return the updated startup step with the added tag
     */
    @Override
	public StartupStep tag(String key, Supplier<String> value) {
		return tag(key, value.get());
	}

	/**
     * Adds a tag to the current startup step.
     * 
     * @param key   the key of the tag
     * @param value the value of the tag
     * @return the current startup step
     * @throws IllegalStateException if the startup step has already ended
     */
    @Override
	public StartupStep tag(String key, String value) {
		Assert.state(!this.ended.get(), "StartupStep has already ended.");
		this.tags.add(new DefaultTag(key, value));
		return this;
	}

	/**
     * Ends the execution of the BufferedStartupStep.
     * Sets the 'ended' flag to true and calls the 'recorder' function to accept this step.
     */
    @Override
	public void end() {
		this.ended.set(true);
		this.recorder.accept(this);
	}

	/**
     * Returns a boolean value indicating whether the BufferedStartupStep has ended.
     *
     * @return true if the BufferedStartupStep has ended, false otherwise.
     */
    boolean isEnded() {
		return this.ended.get();
	}

	/**
     * DefaultTag class.
     */
    static class DefaultTag implements Tag {

		private final String key;

		private final String value;

		/**
         * Constructs a new DefaultTag with the specified key and value.
         *
         * @param key   the key of the tag
         * @param value the value of the tag
         */
        DefaultTag(String key, String value) {
			this.key = key;
			this.value = value;
		}

		/**
         * Returns the key associated with this DefaultTag.
         *
         * @return the key associated with this DefaultTag
         */
        @Override
		public String getKey() {
			return this.key;
		}

		/**
         * Returns the value of the DefaultTag.
         *
         * @return the value of the DefaultTag
         */
        @Override
		public String getValue() {
			return this.value;
		}

	}

}
