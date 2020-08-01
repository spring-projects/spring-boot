/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.buildpack.platform.build;

import java.io.PrintStream;
import java.util.function.Consumer;

import org.springframework.boot.buildpack.platform.docker.LogUpdateEvent;
import org.springframework.boot.buildpack.platform.docker.TotalProgressEvent;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.VolumeName;

/**
 * Callback interface used to provide {@link Builder} output logging.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 * @see #toSystemOut()
 */
public interface BuildLog {

	/**
	 * Log that a build is starting.
	 * @param request the build request
	 */
	void start(BuildRequest request);

	/**
	 * Log that the builder image is being pulled.
	 * @param request the build request
	 * @param imageReference the builder image reference
	 * @return a consumer for progress update events
	 */
	Consumer<TotalProgressEvent> pullingBuilder(BuildRequest request, ImageReference imageReference);

	/**
	 * Log that the builder image has been pulled.
	 * @param request the build request
	 * @param image the builder image that was pulled
	 */
	void pulledBuilder(BuildRequest request, Image image);

	/**
	 * Log that a run image is being pulled.
	 * @param request the build request
	 * @param imageReference the run image reference
	 * @return a consumer for progress update events
	 */
	Consumer<TotalProgressEvent> pullingRunImage(BuildRequest request, ImageReference imageReference);

	/**
	 * Log that a run image has been pulled.
	 * @param request the build request
	 * @param image the run image that was pulled
	 */
	void pulledRunImage(BuildRequest request, Image image);

	/**
	 * Log that the lifecycle is executing.
	 * @param request the build request
	 * @param version the lifecycle version
	 * @param buildCacheVolume the name of the build cache volume in use
	 */
	void executingLifecycle(BuildRequest request, LifecycleVersion version, VolumeName buildCacheVolume);

	/**
	 * Log that a specific phase is running.
	 * @param request the build request
	 * @param name the name of the phase
	 * @return a consumer for log updates
	 */
	Consumer<LogUpdateEvent> runningPhase(BuildRequest request, String name);

	/**
	 * Log that a specific phase is being skipped.
	 * @param name the name of the phase
	 * @param reason the reason the phase is skipped
	 */
	void skippingPhase(String name, String reason);

	/**
	 * Log that the lifecycle has executed.
	 * @param request the build request
	 */
	void executedLifecycle(BuildRequest request);

	/**
	 * Factory method that returns a {@link BuildLog} the outputs to {@link System#out}.
	 * @return a build log instance that logs to system out
	 */
	static BuildLog toSystemOut() {
		return to(System.out);
	}

	/**
	 * Factory method that returns a {@link BuildLog} the outputs to a given
	 * {@link PrintStream}.
	 * @param out the print stream used to output the log
	 * @return a build log instance that logs to the given print stream
	 */
	static BuildLog to(PrintStream out) {
		return new PrintStreamBuildLog(out);
	}

}
