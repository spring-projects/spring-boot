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

package org.springframework.boot.buildpack.platform.build;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.boot.buildpack.platform.docker.LogUpdateEvent;
import org.springframework.boot.buildpack.platform.docker.TotalProgressEvent;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.VolumeName;

/**
 * Base class for {@link BuildLog} implementations.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Andrey Shlykov
 * @author Rafael Ceccone
 * @since 2.3.0
 */
public abstract class AbstractBuildLog implements BuildLog {

	/**
	 * Starts the build process for the specified image.
	 * @param request the build request containing the image name
	 */
	@Override
	public void start(BuildRequest request) {
		log("Building image '" + request.getName() + "'");
		log();
	}

	/**
	 * Returns a Consumer that logs the progress of pulling an image.
	 * @param imageReference the reference of the image being pulled
	 * @param imageType the type of the image being pulled
	 * @return a Consumer that logs the progress of pulling the image
	 */
	@Override
	public Consumer<TotalProgressEvent> pullingImage(ImageReference imageReference, ImageType imageType) {
		return getProgressConsumer(String.format(" > Pulling %s '%s'", imageType.getDescription(), imageReference));
	}

	/**
	 * This method is called when an image is pulled.
	 * @param image The pulled image.
	 * @param imageType The type of the pulled image.
	 */
	@Override
	public void pulledImage(Image image, ImageType imageType) {
		log(String.format(" > Pulled %s '%s'", imageType.getDescription(), getDigest(image)));
	}

	/**
	 * Returns a Consumer that handles TotalProgressEvent for pushing an image.
	 * @param imageReference the reference of the image being pushed
	 * @return a Consumer that handles TotalProgressEvent for pushing the image
	 */
	@Override
	public Consumer<TotalProgressEvent> pushingImage(ImageReference imageReference) {
		return getProgressConsumer(String.format(" > Pushing image '%s'", imageReference));
	}

	/**
	 * Called when an image is pushed.
	 * @param imageReference the reference to the pushed image
	 */
	@Override
	public void pushedImage(ImageReference imageReference) {
		log(String.format(" > Pushed image '%s'", imageReference));
	}

	/**
	 * Executes the lifecycle for a build request.
	 * @param request the build request
	 * @param version the lifecycle version
	 * @param buildCacheVolume the build cache volume
	 */
	@Override
	public void executingLifecycle(BuildRequest request, LifecycleVersion version, VolumeName buildCacheVolume) {
		log(" > Executing lifecycle version " + version);
		log(" > Using build cache volume '" + buildCacheVolume + "'");
	}

	/**
	 * Executes the lifecycle for a given build request.
	 * @param request the build request to execute
	 * @param version the version of the lifecycle to execute
	 * @param buildCache the build cache to use
	 */
	@Override
	public void executingLifecycle(BuildRequest request, LifecycleVersion version, Cache buildCache) {
		log(" > Executing lifecycle version " + version);
		log(" > Using build cache " + buildCache);
	}

	/**
	 * Returns a Consumer that logs the running phase of a build request.
	 * @param request the build request
	 * @param name the name of the running phase
	 * @return a Consumer that logs the running phase
	 */
	@Override
	public Consumer<LogUpdateEvent> runningPhase(BuildRequest request, String name) {
		log();
		log(" > Running " + name);
		String prefix = String.format("    %-14s", "[" + name + "] ");
		return (event) -> log(prefix + event);
	}

	/**
	 * Logs the skipping phase with the given name and reason.
	 * @param name the name of the phase being skipped
	 * @param reason the reason for skipping the phase
	 */
	@Override
	public void skippingPhase(String name, String reason) {
		log();
		log(" > Skipping " + name + " " + reason);
		log();
	}

	/**
	 * Executes the lifecycle of a build request.
	 * @param request the build request to be executed
	 */
	@Override
	public void executedLifecycle(BuildRequest request) {
		log();
		log("Successfully built image '" + request.getName() + "'");
		log();
	}

	/**
	 * This method is called when an image is successfully tagged.
	 * @param tag The reference to the tagged image.
	 */
	@Override
	public void taggedImage(ImageReference tag) {
		log("Successfully created image tag '" + tag + "'");
		log();
	}

	/**
	 * Returns the digest of the given image.
	 * @param image the image for which the digest is to be retrieved
	 * @return the digest of the image, or an empty string if no digest is available
	 */
	private String getDigest(Image image) {
		List<String> digests = image.getDigests();
		return (digests.isEmpty() ? "" : digests.get(0));
	}

	/**
	 * Logs an empty message to the build log.
	 * @param none
	 * @return void
	 */
	protected void log() {
		log("");
	}

	/**
	 * Logs the specified message.
	 * @param message the message to be logged
	 */
	protected abstract void log(String message);

	/**
	 * Returns a consumer that handles total progress events with the given message.
	 * @param message the message to be associated with the progress events
	 * @return a consumer that handles total progress events
	 */
	protected abstract Consumer<TotalProgressEvent> getProgressConsumer(String message);

}
