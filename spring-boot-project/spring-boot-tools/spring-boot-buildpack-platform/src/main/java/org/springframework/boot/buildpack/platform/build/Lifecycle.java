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

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.LogUpdateEvent;
import org.springframework.boot.buildpack.platform.docker.type.ContainerConfig;
import org.springframework.boot.buildpack.platform.docker.type.ContainerContent;
import org.springframework.boot.buildpack.platform.docker.type.ContainerReference;
import org.springframework.boot.buildpack.platform.docker.type.ContainerStatus;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.VolumeName;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.util.Assert;

/**
 * A buildpack lifecycle used to run the build {@link Phase phases} needed to package an
 * application.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class Lifecycle implements Closeable {

	private static final LifecycleVersion LOGGING_MINIMUM_VERSION = LifecycleVersion.parse("0.0.5");

	private final BuildLog log;

	private final DockerApi docker;

	private final BuildRequest request;

	private final EphemeralBuilder builder;

	private final LifecycleVersion lifecycleVersion;

	private final ApiVersion platformVersion;

	private final VolumeName layersVolume;

	private final VolumeName applicationVolume;

	private final VolumeName buildCacheVolume;

	private final VolumeName launchCacheVolume;

	private boolean executed;

	private boolean applicationVolumePopulated;

	/**
	 * Create a new {@link Lifecycle} instance.
	 * @param log build output log
	 * @param docker the Docker API
	 * @param request the request to process
	 * @param builder the ephemeral builder used to run the phases
	 */
	Lifecycle(BuildLog log, DockerApi docker, BuildRequest request, EphemeralBuilder builder) {
		this.log = log;
		this.docker = docker;
		this.request = request;
		this.builder = builder;
		this.lifecycleVersion = LifecycleVersion.parse(builder.getBuilderMetadata().getLifecycle().getVersion());
		this.platformVersion = ApiVersion.parse(builder.getBuilderMetadata().getLifecycle().getApi().getPlatform());
		this.layersVolume = createRandomVolumeName("pack-layers-");
		this.applicationVolume = createRandomVolumeName("pack-app-");
		this.buildCacheVolume = createCacheVolumeName(request, ".build");
		this.launchCacheVolume = createCacheVolumeName(request, ".launch");
		checkPlatformVersion(this.platformVersion);
	}

	protected VolumeName createRandomVolumeName(String prefix) {
		return VolumeName.random(prefix);
	}

	private VolumeName createCacheVolumeName(BuildRequest request, String suffix) {
		return VolumeName.basedOn(request.getName(), ImageReference::toLegacyString, "pack-cache-", suffix, 6);
	}

	private void checkPlatformVersion(ApiVersion platformVersion) {
		ApiVersions.SUPPORTED_PLATFORMS.assertSupports(platformVersion);
	}

	/**
	 * Execute this lifecycle by running each phase in turn.
	 * @throws IOException on IO error
	 */
	void execute() throws IOException {
		Assert.state(!this.executed, "Lifecycle has already been executed");
		this.executed = true;
		this.log.executingLifecycle(this.request, this.lifecycleVersion, this.buildCacheVolume);
		if (this.request.isCleanCache()) {
			deleteVolume(this.buildCacheVolume);
		}
		run(createPhase());
		this.log.executedLifecycle(this.request);
	}

	private Phase createPhase() {
		Phase phase = new Phase("creator", isVerboseLogging());
		phase.withDaemonAccess();
		phase.withLogLevelArg();
		phase.withArgs("-app", Directory.APPLICATION);
		phase.withArgs("-platform", Directory.PLATFORM);
		phase.withArgs("-run-image", this.request.getRunImage());
		phase.withArgs("-layers", Directory.LAYERS);
		phase.withArgs("-cache-dir", Directory.CACHE);
		phase.withArgs("-launch-cache", Directory.LAUNCH_CACHE);
		phase.withArgs("-daemon");
		if (this.request.isCleanCache()) {
			phase.withArgs("-skip-restore");
		}
		phase.withArgs(this.request.getName());
		phase.withBinds(this.layersVolume, Directory.LAYERS);
		phase.withBinds(this.applicationVolume, Directory.APPLICATION);
		phase.withBinds(this.buildCacheVolume, Directory.CACHE);
		phase.withBinds(this.launchCacheVolume, Directory.LAUNCH_CACHE);
		return phase;
	}

	private boolean isVerboseLogging() {
		return this.request.isVerboseLogging() && this.lifecycleVersion.isEqualOrGreaterThan(LOGGING_MINIMUM_VERSION);
	}

	private void run(Phase phase) throws IOException {
		Consumer<LogUpdateEvent> logConsumer = this.log.runningPhase(this.request, phase.getName());
		ContainerConfig containerConfig = ContainerConfig.of(this.builder.getName(), phase::apply);
		ContainerReference reference = createContainer(containerConfig);
		try {
			this.docker.container().start(reference);
			this.docker.container().logs(reference, logConsumer::accept);
			ContainerStatus status = this.docker.container().wait(reference);
			if (status.getStatusCode() != 0) {
				throw new BuilderException(phase.getName(), status.getStatusCode());
			}
		}
		finally {
			this.docker.container().remove(reference, true);
		}
	}

	private ContainerReference createContainer(ContainerConfig config) throws IOException {
		if (this.applicationVolumePopulated) {
			return this.docker.container().create(config);
		}
		try {
			TarArchive applicationContent = this.request.getApplicationContent(this.builder.getBuildOwner());
			return this.docker.container().create(config,
					ContainerContent.of(applicationContent, Directory.APPLICATION));
		}
		finally {
			this.applicationVolumePopulated = true;
		}
	}

	@Override
	public void close() throws IOException {
		deleteVolume(this.layersVolume);
		deleteVolume(this.applicationVolume);
	}

	private void deleteVolume(VolumeName name) throws IOException {
		this.docker.volume().delete(name, true);
	}

	/**
	 * Common directories used by the various phases.
	 */
	private static class Directory {

		/**
		 * The directory used by buildpacks to write their layer contributions. A new
		 * layer directory is created for each lifecycle execution.
		 * <p>
		 * Maps to the {@code <layers...>} concept in the
		 * <a href="https://github.com/buildpacks/spec/blob/master/buildpack.md">buildpack
		 * specification</a> and the {@code -layers} argument from the reference lifecycle
		 * implementation.
		 */
		static final String LAYERS = "/layers";

		/**
		 * The directory containing the original contributed application. A new
		 * application directory is created for each lifecycle execution.
		 * <p>
		 * Maps to the {@code <app...>} concept in the
		 * <a href="https://github.com/buildpacks/spec/blob/master/buildpack.md">buildpack
		 * specification</a> and the {@code -app} argument from the reference lifecycle
		 * implementation. The reference lifecycle follows the Kubernetes/Docker
		 * convention of using {@code '/workspace'}.
		 * <p>
		 * Note that application content is uploaded to the container with the first phase
		 * that runs and saved in a volume that is passed to subsequent phases. The
		 * directory is mutable and buildpacks may modify the content.
		 */
		static final String APPLICATION = "/workspace";

		/**
		 * The directory used by buildpacks to obtain environment variables and platform
		 * specific concerns. The platform directory is read-only and is created/populated
		 * by the {@link EphemeralBuilder}.
		 * <p>
		 * Maps to the {@code <platform>/env} and {@code <platform>/#} concepts in the
		 * <a href="https://github.com/buildpacks/spec/blob/master/buildpack.md">buildpack
		 * specification</a> and the {@code -platform} argument from the reference
		 * lifecycle implementation.
		 */
		static final String PLATFORM = "/platform";

		/**
		 * The directory used by buildpacks for caching. The volume name is based on the
		 * image {@link BuildRequest#getName() name} being built, and is persistent across
		 * invocations even if the application content has changed.
		 * <p>
		 * Maps to the {@code -path} argument from the reference lifecycle implementation
		 * cache and restore phases
		 */
		static final String CACHE = "/cache";

		/**
		 * The directory used by buildpacks for launch related caching. The volume name is
		 * based on the image {@link BuildRequest#getName() name} being built, and is
		 * persistent across invocations even if the application content has changed.
		 * <p>
		 * Maps to the {@code -launch-cache} argument from the reference lifecycle
		 * implementation export phase
		 */
		static final String LAUNCH_CACHE = "/launch-cache";

	}

}
