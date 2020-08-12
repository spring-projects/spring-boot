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
 * @since 2.3.0
 */
public abstract class AbstractBuildLog implements BuildLog {

	@Override
	public void start(BuildRequest request) {
		log("Building image '" + request.getName() + "'");
		log();
	}

	@Override
	@Deprecated
	public Consumer<TotalProgressEvent> pullingBuilder(BuildRequest request, ImageReference imageReference) {
		return pullingImage(imageReference, ImageType.BUILDER);
	}

	@Override
	@Deprecated
	public void pulledBuilder(BuildRequest request, Image image) {
		pulledImage(image, ImageType.BUILDER);
	}

	@Override
	@Deprecated
	public Consumer<TotalProgressEvent> pullingRunImage(BuildRequest request, ImageReference imageReference) {
		return pullingImage(imageReference, ImageType.RUNNER);
	}

	@Override
	@Deprecated
	public void pulledRunImage(BuildRequest request, Image image) {
		pulledImage(image, ImageType.RUNNER);
	}

	@Override
	public Consumer<TotalProgressEvent> pullingImage(ImageReference imageReference, ImageType imageType) {
		return getProgressConsumer(String.format(" > Pulling %s '%s'", imageType.getDescription(), imageReference));
	}

	@Override
	public void pulledImage(Image image, ImageType imageType) {
		log(String.format(" > Pulled %s '%s'", imageType.getDescription(), getDigest(image)));
	}

	@Override
	public void executingLifecycle(BuildRequest request, LifecycleVersion version, VolumeName buildCacheVolume) {
		log(" > Executing lifecycle version " + version);
		log(" > Using build cache volume '" + buildCacheVolume + "'");
	}

	@Override
	public Consumer<LogUpdateEvent> runningPhase(BuildRequest request, String name) {
		log();
		log(" > Running " + name);
		String prefix = String.format("    %-14s", "[" + name + "] ");
		return (event) -> log(prefix + event);
	}

	@Override
	public void skippingPhase(String name, String reason) {
		log();
		log(" > Skipping " + name + " " + reason);
		log();
	}

	@Override
	public void executedLifecycle(BuildRequest request) {
		log();
		log("Successfully built image '" + request.getName() + "'");
		log();
	}

	private String getDigest(Image image) {
		List<String> digests = image.getDigests();
		return (digests.isEmpty() ? "" : digests.get(0));
	}

	protected void log() {
		log("");
	}

	protected abstract void log(String message);

	protected abstract Consumer<TotalProgressEvent> getProgressConsumer(String message);

}
