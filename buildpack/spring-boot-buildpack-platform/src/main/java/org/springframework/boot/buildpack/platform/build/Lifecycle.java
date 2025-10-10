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

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.sun.jna.Platform;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.build.Cache.Bind;
import org.springframework.boot.buildpack.platform.docker.ApiVersion;
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
import org.springframework.util.FileSystemUtils;

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

	private static final List<String> DEFAULT_SECURITY_OPTIONS = List.of("label=disable");

	private final BuildLog log;

	private final DockerApi docker;

	private final @Nullable ResolvedDockerHost dockerHost;

	private final BuildRequest request;

	private final EphemeralBuilder builder;

	private final LifecycleVersion lifecycleVersion;

	private final ApiVersion platformVersion;

	private final Cache layers;

	private final Cache application;

	private final Cache buildCache;

	private final Cache launchCache;

	private final String applicationDirectory;

	private final List<String> securityOptions;

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
	Lifecycle(BuildLog log, DockerApi docker, @Nullable ResolvedDockerHost dockerHost, BuildRequest request,
			EphemeralBuilder builder) {
		this.log = log;
		this.docker = docker;
		this.dockerHost = dockerHost;
		this.request = request;
		this.builder = builder;
		this.lifecycleVersion = LifecycleVersion.parse(builder.getBuilderMetadata().getLifecycle().getVersion());
		this.platformVersion = getPlatformVersion(builder.getBuilderMetadata().getLifecycle());
		this.layers = getLayersBindingSource(request);
		this.application = getApplicationBindingSource(request);
		this.buildCache = getBuildCache(request);
		this.launchCache = getLaunchCache(request);
		this.applicationDirectory = getApplicationDirectory(request);
		this.securityOptions = getSecurityOptions(request);
	}

	String getApplicationDirectory() {
		return this.applicationDirectory;
	}

	private Cache getBuildCache(BuildRequest request) {
		if (request.getBuildCache() != null) {
			return request.getBuildCache();
		}
		return createVolumeCache(request, "build");
	}

	private Cache getLaunchCache(BuildRequest request) {
		if (request.getLaunchCache() != null) {
			return request.getLaunchCache();
		}
		return createVolumeCache(request, "launch");
	}

	private String getApplicationDirectory(BuildRequest request) {
		return (request.getApplicationDirectory() != null) ? request.getApplicationDirectory() : Directory.APPLICATION;
	}

	private List<String> getSecurityOptions(BuildRequest request) {
		if (request.getSecurityOptions() != null) {
			return request.getSecurityOptions();
		}
		return (Platform.isWindows()) ? Collections.emptyList() : DEFAULT_SECURITY_OPTIONS;
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
		this.log.executingLifecycle(this.request, this.lifecycleVersion, this.buildCache);
		if (this.request.isCleanCache()) {
			deleteCache(this.buildCache);
		}
		if (this.request.isTrustBuilder()) {
			run(createPhase());
		}
		else {
			run(analyzePhase());
			run(detectPhase());
			if (!this.request.isCleanCache()) {
				run(restorePhase());
			}
			else {
				this.log.skippingPhase("restorer", "because 'cleanCache' is enabled");
			}
			run(buildPhase());
			run(exportPhase());
		}
		this.log.executedLifecycle(this.request);
	}

	private Phase createPhase() {
		Phase phase = new Phase("creator", isVerboseLogging());
		phase.withApp(this.applicationDirectory,
				Binding.from(getCacheBindingSource(this.application), this.applicationDirectory));
		phase.withPlatform(Directory.PLATFORM);
		ImageReference runImage = this.request.getRunImage();
		Assert.state(runImage != null, "'runImage' must not be null");
		phase.withRunImage(runImage);
		phase.withLayers(Directory.LAYERS, Binding.from(getCacheBindingSource(this.layers), Directory.LAYERS));
		phase.withBuildCache(Directory.CACHE, Binding.from(getCacheBindingSource(this.buildCache), Directory.CACHE));
		phase.withLaunchCache(Directory.LAUNCH_CACHE,
				Binding.from(getCacheBindingSource(this.launchCache), Directory.LAUNCH_CACHE));
		configureDaemonAccess(phase);
		if (this.request.isCleanCache()) {
			phase.withSkipRestore();
		}
		if (requiresProcessTypeDefault()) {
			phase.withProcessType("web");
		}
		phase.withImageName(this.request.getName());
		configureOptions(phase);
		configureCreatedDate(phase);
		return phase;

	}

	private Phase analyzePhase() {
		Phase phase = new Phase("analyzer", isVerboseLogging());
		configureDaemonAccess(phase);
		phase.withLaunchCache(Directory.LAUNCH_CACHE,
				Binding.from(getCacheBindingSource(this.launchCache), Directory.LAUNCH_CACHE));
		phase.withLayers(Directory.LAYERS, Binding.from(getCacheBindingSource(this.layers), Directory.LAYERS));
		ImageReference runImage = this.request.getRunImage();
		Assert.state(runImage != null, "'runImage' must not be null");
		phase.withRunImage(runImage);
		phase.withImageName(this.request.getName());
		configureOptions(phase);
		return phase;
	}

	private Phase detectPhase() {
		Phase phase = new Phase("detector", isVerboseLogging());
		phase.withApp(this.applicationDirectory,
				Binding.from(getCacheBindingSource(this.application), this.applicationDirectory));
		phase.withLayers(Directory.LAYERS, Binding.from(getCacheBindingSource(this.layers), Directory.LAYERS));
		phase.withPlatform(Directory.PLATFORM);
		configureOptions(phase);
		return phase;
	}

	private Phase restorePhase() {
		Phase phase = new Phase("restorer", isVerboseLogging());
		configureDaemonAccess(phase);
		phase.withBuildCache(Directory.CACHE, Binding.from(getCacheBindingSource(this.buildCache), Directory.CACHE));
		phase.withLayers(Directory.LAYERS, Binding.from(getCacheBindingSource(this.layers), Directory.LAYERS));
		configureOptions(phase);
		return phase;
	}

	private Phase buildPhase() {
		Phase phase = new Phase("builder", isVerboseLogging());
		phase.withApp(this.applicationDirectory,
				Binding.from(getCacheBindingSource(this.application), this.applicationDirectory));
		phase.withLayers(Directory.LAYERS, Binding.from(getCacheBindingSource(this.layers), Directory.LAYERS));
		phase.withPlatform(Directory.PLATFORM);
		configureOptions(phase);
		return phase;
	}

	private Phase exportPhase() {
		Phase phase = new Phase("exporter", isVerboseLogging());
		configureDaemonAccess(phase);
		phase.withApp(this.applicationDirectory,
				Binding.from(getCacheBindingSource(this.application), this.applicationDirectory));
		phase.withBuildCache(Directory.CACHE, Binding.from(getCacheBindingSource(this.buildCache), Directory.CACHE));
		phase.withLaunchCache(Directory.LAUNCH_CACHE,
				Binding.from(getCacheBindingSource(this.launchCache), Directory.LAUNCH_CACHE));
		phase.withLayers(Directory.LAYERS, Binding.from(getCacheBindingSource(this.layers), Directory.LAYERS));
		if (requiresProcessTypeDefault()) {
			phase.withProcessType("web");
		}
		phase.withImageName(this.request.getName());
		configureOptions(phase);
		configureCreatedDate(phase);
		return phase;
	}

	private Cache getLayersBindingSource(BuildRequest request) {
		if (request.getBuildWorkspace() != null) {
			return getBuildWorkspaceBindingSource(request.getBuildWorkspace(), "layers");
		}
		return createVolumeCache("pack-layers-");
	}

	private Cache getApplicationBindingSource(BuildRequest request) {
		if (request.getBuildWorkspace() != null) {
			return getBuildWorkspaceBindingSource(request.getBuildWorkspace(), "app");
		}
		return createVolumeCache("pack-app-");
	}

	private Cache getBuildWorkspaceBindingSource(Cache buildWorkspace, String suffix) {
		if (buildWorkspace.getVolume() != null) {
			return Cache.volume(buildWorkspace.getVolume().getName() + "-" + suffix);
		}

		Bind bind = buildWorkspace.getBind();
		Assert.state(bind != null, "'bind' must not be null");
		return Cache.bind(bind.getSource() + "-" + suffix);
	}

	private String getCacheBindingSource(Cache cache) {
		if (cache.getVolume() != null) {
			return cache.getVolume().getName();
		}
		Bind bind = cache.getBind();
		Assert.state(bind != null, "'bind' must not be null");
		return bind.getSource();
	}

	private Cache createVolumeCache(String prefix) {
		return Cache.volume(createRandomVolumeName(prefix));
	}

	private Cache createVolumeCache(BuildRequest request, String suffix) {
		return Cache.volume(
				VolumeName.basedOn(request.getName(), ImageReference::toLegacyString, "pack-cache-", "." + suffix, 6));
	}

	protected VolumeName createRandomVolumeName(String prefix) {
		return VolumeName.random(prefix);
	}

	private void configureDaemonAccess(Phase phase) {
		phase.withDaemonAccess();
		if (this.dockerHost != null) {
			if (this.dockerHost.isRemote()) {
				phase.withEnv("DOCKER_HOST", this.dockerHost.getAddress());
				if (this.dockerHost.isSecure()) {
					phase.withEnv("DOCKER_TLS_VERIFY", "1");
					if (this.dockerHost.getCertificatePath() != null) {
						phase.withEnv("DOCKER_CERT_PATH", this.dockerHost.getCertificatePath());
					}
				}
			}
			else {
				phase.withBinding(Binding.from(this.dockerHost.getAddress(), DOMAIN_SOCKET_PATH));
			}
		}
		else {
			phase.withBinding(Binding.from(DOMAIN_SOCKET_PATH, DOMAIN_SOCKET_PATH));
		}
		if (this.securityOptions != null) {
			this.securityOptions.forEach(phase::withSecurityOption);
		}
	}

	private void configureCreatedDate(Phase phase) {
		if (this.request.getCreatedDate() != null) {
			phase.withEnv(SOURCE_DATE_EPOCH_KEY, Long.toString(this.request.getCreatedDate().getEpochSecond()));
		}
	}

	private void configureOptions(Phase phase) {
		if (this.request.getBindings() != null) {
			this.request.getBindings().forEach(phase::withBinding);
		}
		if (this.request.getNetwork() != null) {
			phase.withNetworkMode(this.request.getNetwork());
		}
		phase.withEnv(PLATFORM_API_VERSION_KEY, this.platformVersion.toString());
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
		ContainerReference reference = createContainer(containerConfig, phase.requiresApp());
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

	private ContainerReference createContainer(ContainerConfig config, boolean requiresAppUpload) throws IOException {
		if (!requiresAppUpload || this.applicationVolumePopulated) {
			return this.docker.container().create(config, this.request.getImagePlatform());
		}
		try {
			if (this.application.getBind() != null) {
				Files.createDirectories(Path.of(this.application.getBind().getSource()));
			}
			TarArchive applicationContent = this.request.getApplicationContent(this.builder.getBuildOwner());
			return this.docker.container()
				.create(config, this.request.getImagePlatform(),
						ContainerContent.of(applicationContent, this.applicationDirectory));
		}
		finally {
			this.applicationVolumePopulated = true;
		}
	}

	@Override
	public void close() throws IOException {
		deleteCache(this.layers);
		deleteCache(this.application);
	}

	private void deleteCache(Cache cache) throws IOException {
		if (cache.getVolume() != null) {
			deleteVolume(cache.getVolume().getVolumeName());
		}
		if (cache.getBind() != null) {
			deleteBind(cache.getBind());
		}
	}

	private void deleteVolume(VolumeName name) throws IOException {
		this.docker.volume().delete(name, true);
	}

	private void deleteBind(Cache.Bind bind) {
		try {
			FileSystemUtils.deleteRecursively(Path.of(bind.getSource()));
		}
		catch (Exception ex) {
			this.log.failedCleaningWorkDir(bind, ex);
		}
	}

	/**
	 * Common directories used by the various phases.
	 */
	private static final class Directory {

		/**
		 * The directory used by buildpacks to write their layer contributions. A new
		 * layer directory is created for each lifecycle execution.
		 * <p>
		 * Maps to the {@code <layers...>} concept in the
		 * <a href="https://github.com/buildpacks/spec/blob/master/buildpack.md">buildpack
		 * specification</a> and the {@code -layers} argument to lifecycle phases.
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
		 * specification</a> and the {@code -platform} argument to lifecycle phases.
		 */
		static final String PLATFORM = "/platform";

		/**
		 * The directory used by buildpacks for caching. The volume name is based on the
		 * image {@link BuildRequest#getName() name} being built, and is persistent across
		 * invocations even if the application content has changed.
		 * <p>
		 * Maps to the {@code -path} argument to lifecycle phases.
		 */
		static final String CACHE = "/cache";

		/**
		 * The directory used by buildpacks for launch related caching. The volume name is
		 * based on the image {@link BuildRequest#getName() name} being built, and is
		 * persistent across invocations even if the application content has changed.
		 * <p>
		 * Maps to the {@code -launch-cache} argument to lifecycle phases.
		 */
		static final String LAUNCH_CACHE = "/launch-cache";

	}

}
