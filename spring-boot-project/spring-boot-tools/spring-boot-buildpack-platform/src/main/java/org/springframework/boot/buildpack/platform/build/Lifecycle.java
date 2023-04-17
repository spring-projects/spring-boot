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

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

import com.sun.jna.Platform;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.LogUpdateEvent;
import org.springframework.boot.buildpack.platform.docker.configuration.ResolvedDockerHost;
import org.springframework.boot.buildpack.platform.docker.type.Binding;
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
 * @author Jeroen Meijer
 * @author Julian Liebig
 */
class Lifecycle implements Closeable {

	private static final LifecycleVersion LOGGING_MINIMUM_VERSION = LifecycleVersion.parse("0.0.5");

	private static final String PLATFORM_API_VERSION_KEY = "CNB_PLATFORM_API";

	private static final String SOURCE_DATE_EPOCH_KEY = "SOURCE_DATE_EPOCH";

	private static final String DOMAIN_SOCKET_PATH = "/var/run/docker.sock";

	private final BuildLog log;

	private final DockerApi docker;

	private final ResolvedDockerHost dockerHost;

	private final BuildRequest request;

	private final EphemeralBuilder builder;

	private final LifecycleVersion lifecycleVersion;

	private final ApiVersion platformVersion;

	private final VolumeName layersVolume;

	private final VolumeName applicationVolume;

	private final VolumeName buildCacheVolume;

	private final VolumeName launchCacheVolume;

	private final String applicationDirectory;

	private boolean executed;

	private boolean applicationVolumePopulated;

	/**
	 * Create a new {@link Lifecycle} instance.
	 * @param log build output log
	 * @param docker the Docker API
	 * @param dockerHost the Docker host information
	 * @param request the request to process
	 * @param builder the ephemeral builder used to run the phases
	 */
	Lifecycle(BuildLog log, DockerApi docker, ResolvedDockerHost dockerHost, BuildRequest request,
			EphemeralBuilder builder) {
		this.log = log;
		this.docker = docker;
		this.dockerHost = dockerHost;
		this.request = request;
		this.builder = builder;
		this.lifecycleVersion = LifecycleVersion.parse(builder.getBuilderMetadata().getLifecycle().getVersion());
		this.platformVersion = getPlatformVersion(builder.getBuilderMetadata().getLifecycle());
		this.layersVolume = createRandomVolumeName("pack-layers-");
		this.applicationVolume = createRandomVolumeName("pack-app-");
		this.buildCacheVolume = getBuildCacheVolumeName(request);
		this.launchCacheVolume = getLaunchCacheVolumeName(request);
		this.applicationDirectory = getApplicationDirectory(request);
	}

	protected VolumeName createRandomVolumeName(String prefix) {
		return VolumeName.random(prefix);
	}

	private VolumeName getBuildCacheVolumeName(BuildRequest request) {
		if (request.getBuildCache() != null) {
			return getVolumeName(request.getBuildCache());
		}
		return createCacheVolumeName(request, "build");
	}

	private VolumeName getLaunchCacheVolumeName(BuildRequest request) {
		if (request.getLaunchCache() != null) {
			return getVolumeName(request.getLaunchCache());
		}
		return createCacheVolumeName(request, "launch");
	}

	private VolumeName getVolumeName(Cache cache) {
		if (cache.getVolume() != null) {
			return VolumeName.of(cache.getVolume().getName());
		}
		return null;
	}

	private String getApplicationDirectory(BuildRequest request) {
		return (request.getApplicationDirectory() != null) ? request.getApplicationDirectory() : Directory.APPLICATION;
	}

	private VolumeName createCacheVolumeName(BuildRequest request, String suffix) {
		return VolumeName.basedOn(request.getName(), ImageReference::toLegacyString, "pack-cache-", "." + suffix, 6);
	}

	private ApiVersion getPlatformVersion(BuilderMetadata.Lifecycle lifecycle) {
		if (lifecycle.getApis().getPlatform() != null) {
			String[] supportedVersions = lifecycle.getApis().getPlatform();
			return ApiVersions.SUPPORTED_PLATFORMS.findLatestSupported(supportedVersions);
		}
		String version = lifecycle.getApi().getPlatform();
		return ApiVersions.SUPPORTED_PLATFORMS.findLatestSupported(version);
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
		configureDaemonAccess(phase);
		phase.withLogLevelArg();
		phase.withArgs("-app", this.applicationDirectory);
		phase.withArgs("-platform", Directory.PLATFORM);
		phase.withArgs("-run-image", this.request.getRunImage());
		phase.withArgs("-layers", Directory.LAYERS);
		phase.withArgs("-cache-dir", Directory.CACHE);
		phase.withArgs("-launch-cache", Directory.LAUNCH_CACHE);
		phase.withArgs("-daemon");
		if (this.request.isCleanCache()) {
			phase.withArgs("-skip-restore");
		}
		if (requiresProcessTypeDefault()) {
			phase.withArgs("-process-type=web");
		}
		phase.withArgs(this.request.getName());
		phase.withBinding(Binding.from(this.layersVolume, Directory.LAYERS));
		phase.withBinding(Binding.from(this.applicationVolume, this.applicationDirectory));
		phase.withBinding(Binding.from(this.buildCacheVolume, Directory.CACHE));
		phase.withBinding(Binding.from(this.launchCacheVolume, Directory.LAUNCH_CACHE));
		if (this.request.getBindings() != null) {
			this.request.getBindings().forEach(phase::withBinding);
		}
		phase.withEnv(PLATFORM_API_VERSION_KEY, this.platformVersion.toString());
		if (this.request.getNetwork() != null) {
			phase.withNetworkMode(this.request.getNetwork());
		}
		if (this.request.getCreatedDate() != null) {
			phase.withEnv(SOURCE_DATE_EPOCH_KEY, Long.toString(this.request.getCreatedDate().getEpochSecond()));
		}
		return phase;
	}

	private void configureDaemonAccess(Phase phase) {
		if (this.dockerHost != null) {
			if (this.dockerHost.isRemote()) {
				phase.withEnv("DOCKER_HOST", this.dockerHost.getAddress());
				if (this.dockerHost.isSecure()) {
					phase.withEnv("DOCKER_TLS_VERIFY", "1");
					phase.withEnv("DOCKER_CERT_PATH", this.dockerHost.getCertificatePath());
				}
			}
			else {
				phase.withBinding(Binding.from(this.dockerHost.getAddress(), DOMAIN_SOCKET_PATH));
			}
		}
		else {
			phase.withBinding(Binding.from(DOMAIN_SOCKET_PATH, DOMAIN_SOCKET_PATH));
		}
		if (!Platform.isWindows()) {
			phase.withSecurityOption("label=disable");
		}
	}

	private boolean isVerboseLogging() {
		return this.request.isVerboseLogging() && this.lifecycleVersion.isEqualOrGreaterThan(LOGGING_MINIMUM_VERSION);
	}

	private boolean requiresProcessTypeDefault() {
		return this.platformVersion.supportsAny(ApiVersion.of(0, 4), ApiVersion.of(0, 5));
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
			return this.docker.container()
				.create(config, ContainerContent.of(applicationContent, this.applicationDirectory));
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
