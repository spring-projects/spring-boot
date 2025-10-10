/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.docker.ImagePlatform;
import org.springframework.boot.buildpack.platform.docker.LogUpdateEvent;
import org.springframework.boot.buildpack.platform.docker.TotalProgressEvent;
import org.springframework.boot.buildpack.platform.docker.type.Binding;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.VolumeName;

/**
 * Callback interface used to provide {@link Builder} output logging.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Andrey Shlykov
 * @author Rafael Ceccone
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
	 * Log that an image is being pulled.
	 * @param imageReference the image reference
	 * @param platform the platform of the image
	 * @param imageType the image type
	 * @return a consumer for progress update events
	 */
	Consumer<TotalProgressEvent> pullingImage(ImageReference imageReference, @Nullable ImagePlatform platform,
			ImageType imageType);

	/**
	 * Log that an image has been pulled.
	 * @param image the image that was pulled
	 * @param imageType the image type that was pulled
	 */
	void pulledImage(Image image, ImageType imageType);

	/**
	 * Log that an image is being pushed.
	 * @param imageReference the image reference
	 * @return a consumer for progress update events
	 */
	Consumer<TotalProgressEvent> pushingImage(ImageReference imageReference);

	/**
	 * Log that an image has been pushed.
	 * @param imageReference the image reference
	 */
	void pushedImage(ImageReference imageReference);

	/**
	 * Log that the lifecycle is executing.
	 * @param request the build request
	 * @param version the lifecycle version
	 * @param buildCacheVolume the name of the build cache volume in use
	 */
	void executingLifecycle(BuildRequest request, LifecycleVersion version, VolumeName buildCacheVolume);

	/**
	 * Log that the lifecycle is executing.
	 * @param request the build request
	 * @param version the lifecycle version
	 * @param buildCache the build cache in use
	 */
	void executingLifecycle(BuildRequest request, LifecycleVersion version, Cache buildCache);

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
	 * Log that a tag has been created.
	 * @param tag the tag reference
	 */
	void taggedImage(ImageReference tag);

	/**
	 * Log that a cache cleanup step was not completed successfully.
	 * @param cache the cache
	 * @param exception any exception that caused the failure
	 * @since 3.2.6
	 */
	void failedCleaningWorkDir(Cache cache, @Nullable Exception exception);

	/**
	 * Log that a binding with a sensitive target has been detected.
	 * @param binding the binding
	 * @since 3.4.0
	 */
	void sensitiveTargetBindingDetected(Binding binding);

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
