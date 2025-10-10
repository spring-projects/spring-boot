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

import java.util.List;
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
 * Base class for {@link BuildLog} implementations.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Andrey Shlykov
 * @author Rafael Ceccone
 * @since 2.3.0
 */
public abstract class AbstractBuildLog implements BuildLog {

	@Override
	public void start(BuildRequest request) {
		log("Building image '" + request.getName() + "'");
		log();
	}

	@Override
	public Consumer<TotalProgressEvent> pullingImage(ImageReference imageReference, @Nullable ImagePlatform platform,
			ImageType imageType) {
		return (platform != null)
				? getProgressConsumer(" > Pulling %s '%s' for platform '%s'".formatted(imageType.getDescription(),
						imageReference, platform))
				: getProgressConsumer(" > Pulling %s '%s'".formatted(imageType.getDescription(), imageReference));
	}

	@Override
	public void pulledImage(Image image, ImageType imageType) {
		log(String.format(" > Pulled %s '%s'", imageType.getDescription(), getDigest(image)));
	}

	@Override
	public Consumer<TotalProgressEvent> pushingImage(ImageReference imageReference) {
		return getProgressConsumer(String.format(" > Pushing image '%s'", imageReference));
	}

	@Override
	public void pushedImage(ImageReference imageReference) {
		log(String.format(" > Pushed image '%s'", imageReference));
	}

	@Override
	public void executingLifecycle(BuildRequest request, LifecycleVersion version, VolumeName buildCacheVolume) {
		log(" > Executing lifecycle version " + version);
		log(" > Using build cache volume '" + buildCacheVolume + "'");
	}

	@Override
	public void executingLifecycle(BuildRequest request, LifecycleVersion version, Cache buildCache) {
		log(" > Executing lifecycle version " + version);
		log(" > Using build cache " + buildCache);
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

	@Override
	public void taggedImage(ImageReference tag) {
		log("Successfully created image tag '" + tag + "'");
		log();
	}

	@Override
	public void failedCleaningWorkDir(Cache cache, @Nullable Exception exception) {
		StringBuilder message = new StringBuilder("Warning: Working location " + cache + " could not be cleaned");
		if (exception != null) {
			message.append(": ").append(exception.getMessage());
		}
		log();
		log(message.toString());
		log();
	}

	@Override
	public void sensitiveTargetBindingDetected(Binding binding) {
		log("Warning: Binding '%s' uses a container path which is used by buildpacks while building. Binding to it can cause problems!"
			.formatted(binding));
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
