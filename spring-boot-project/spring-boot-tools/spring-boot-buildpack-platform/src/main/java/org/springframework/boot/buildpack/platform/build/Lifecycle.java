/*
 * Copyright 2012-2024 the original author or authors.
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

	private final ResolvedDockerHost dockerHost;

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
	Lifecycle(BuildLog log, DockerApi docker, ResolvedDockerHost dockerHost, BuildRequest request,
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

	/**
     * Retrieves the build cache for the given build request.
     * 
     * @param request the build request
     * @return the build cache
     */
    private Cache getBuildCache(BuildRequest request) {
		if (request.getBuildCache() != null) {
			return request.getBuildCache();
		}
		return createVolumeCache(request, "build");
	}

	/**
     * Retrieves the launch cache for the given build request.
     * If the launch cache is already set in the request, it is returned.
     * Otherwise, a new launch cache is created using the provided build request and returned.
     *
     * @param request the build request containing the launch cache
     * @return the launch cache for the build request
     */
    private Cache getLaunchCache(BuildRequest request) {
		if (request.getLaunchCache() != null) {
			return request.getLaunchCache();
		}
		return createVolumeCache(request, "launch");
	}

	/**
     * Returns the application directory specified in the given build request.
     * If the application directory is not provided in the request, the default
     * application directory will be returned.
     *
     * @param request the build request containing the application directory
     * @return the application directory specified in the request, or the default
     *         application directory if not provided
     */
    private String getApplicationDirectory(BuildRequest request) {
		return (request.getApplicationDirectory() != null) ? request.getApplicationDirectory() : Directory.APPLICATION;
	}

	/**
     * Returns the security options for the given build request.
     * 
     * @param request the build request
     * @return the security options for the build request
     */
    private List<String> getSecurityOptions(BuildRequest request) {
		if (request.getSecurityOptions() != null) {
			return request.getSecurityOptions();
		}
		return (Platform.isWindows()) ? Collections.emptyList() : DEFAULT_SECURITY_OPTIONS;
	}

	/**
     * Returns the latest supported platform version based on the given lifecycle.
     * 
     * @param lifecycle the lifecycle object containing the APIs information
     * @return the latest supported platform version
     */
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
		run(createPhase());
		this.log.executedLifecycle(this.request);
	}

	/**
     * Creates a new Phase object with the specified configuration.
     * 
     * @return the created Phase object
     */
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
		phase.withBinding(Binding.from(getCacheBindingSource(this.layers), Directory.LAYERS));
		phase.withBinding(Binding.from(getCacheBindingSource(this.application), this.applicationDirectory));
		phase.withBinding(Binding.from(getCacheBindingSource(this.buildCache), Directory.CACHE));
		phase.withBinding(Binding.from(getCacheBindingSource(this.launchCache), Directory.LAUNCH_CACHE));
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

	/**
     * Returns the cache for the layers binding source based on the given build request.
     * If the build workspace is not null, it retrieves the cache from the build workspace using the "layers" key.
     * Otherwise, it creates a new volume cache with the prefix "pack-layers-".
     *
     * @param request the build request containing the build workspace
     * @return the cache for the layers binding source
     */
    private Cache getLayersBindingSource(BuildRequest request) {
		if (request.getBuildWorkspace() != null) {
			return getBuildWorkspaceBindingSource(request.getBuildWorkspace(), "layers");
		}
		return createVolumeCache("pack-layers-");
	}

	/**
     * Retrieves the application binding source from the given build request.
     * If the build workspace is not null, it calls the getBuildWorkspaceBindingSource method
     * with the build workspace and "app" as parameters.
     * Otherwise, it creates a volume cache with the prefix "pack-app-" using the createVolumeCache method.
     * 
     * @param request the build request containing the build workspace
     * @return the application binding source cache
     */
    private Cache getApplicationBindingSource(BuildRequest request) {
		if (request.getBuildWorkspace() != null) {
			return getBuildWorkspaceBindingSource(request.getBuildWorkspace(), "app");
		}
		return createVolumeCache("pack-app-");
	}

	/**
     * Returns a new Cache object based on the given buildWorkspace and suffix.
     * If the buildWorkspace has a volume, a new Cache object is created with the volume name concatenated with the suffix.
     * If the buildWorkspace has a bind, a new Cache object is created with the bind source concatenated with the suffix.
     * 
     * @param buildWorkspace The original Cache object representing the build workspace.
     * @param suffix The suffix to be appended to the volume name or bind source.
     * @return A new Cache object based on the given buildWorkspace and suffix.
     */
    private Cache getBuildWorkspaceBindingSource(Cache buildWorkspace, String suffix) {
		return (buildWorkspace.getVolume() != null) ? Cache.volume(buildWorkspace.getVolume().getName() + "-" + suffix)
				: Cache.bind(buildWorkspace.getBind().getSource() + "-" + suffix);
	}

	/**
     * Returns the binding source of the cache.
     * 
     * @param cache the cache object
     * @return the binding source of the cache
     */
    private String getCacheBindingSource(Cache cache) {
		return (cache.getVolume() != null) ? cache.getVolume().getName() : cache.getBind().getSource();
	}

	/**
     * Creates a cache for a volume with the given prefix.
     * 
     * @param prefix the prefix to be used for the volume name
     * @return the cache for the volume
     */
    private Cache createVolumeCache(String prefix) {
		return Cache.volume(createRandomVolumeName(prefix));
	}

	/**
     * Creates a cache for a volume based on the given build request and suffix.
     * 
     * @param request the build request
     * @param suffix the suffix to be added to the cache name
     * @return the created cache
     */
    private Cache createVolumeCache(BuildRequest request, String suffix) {
		return Cache.volume(
				VolumeName.basedOn(request.getName(), ImageReference::toLegacyString, "pack-cache-", "." + suffix, 6));
	}

	/**
     * Generates a random volume name with the given prefix.
     * 
     * @param prefix the prefix to be used in the volume name
     * @return a randomly generated volume name with the given prefix
     */
    protected VolumeName createRandomVolumeName(String prefix) {
		return VolumeName.random(prefix);
	}

	/**
     * Configures the daemon access for the specified phase.
     * 
     * @param phase The phase to configure the daemon access for.
     */
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
		if (this.securityOptions != null) {
			this.securityOptions.forEach(phase::withSecurityOption);
		}
	}

	/**
     * Checks if verbose logging is enabled.
     * 
     * @return true if verbose logging is enabled, false otherwise
     */
    private boolean isVerboseLogging() {
		return this.request.isVerboseLogging() && this.lifecycleVersion.isEqualOrGreaterThan(LOGGING_MINIMUM_VERSION);
	}

	/**
     * Checks if the current platform version requires the default process type.
     * 
     * @return true if the platform version supports any of the specified API versions (0.4 or 0.5), false otherwise
     */
    private boolean requiresProcessTypeDefault() {
		return this.platformVersion.supportsAny(ApiVersion.of(0, 4), ApiVersion.of(0, 5));
	}

	/**
     * Runs the specified phase of the lifecycle.
     * 
     * @param phase the phase to run
     * @throws IOException if an I/O error occurs
     */
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

	/**
     * Creates a container with the given configuration.
     * 
     * @param config the configuration for the container
     * @return a reference to the created container
     * @throws IOException if an I/O error occurs
     */
    private ContainerReference createContainer(ContainerConfig config) throws IOException {
		if (this.applicationVolumePopulated) {
			return this.docker.container().create(config);
		}
		try {
			if (this.application.getBind() != null) {
				Files.createDirectories(Path.of(this.application.getBind().getSource()));
			}
			TarArchive applicationContent = this.request.getApplicationContent(this.builder.getBuildOwner());
			return this.docker.container()
				.create(config, ContainerContent.of(applicationContent, this.applicationDirectory));
		}
		finally {
			this.applicationVolumePopulated = true;
		}
	}

	/**
     * Closes the Lifecycle by deleting the cache for the layers and application.
     * 
     * @throws IOException if an I/O error occurs while deleting the cache
     */
    @Override
	public void close() throws IOException {
		deleteCache(this.layers);
		deleteCache(this.application);
	}

	/**
     * Deletes the cache by deleting its associated volume and bind.
     * 
     * @param cache the cache to be deleted
     * @throws IOException if an I/O error occurs while deleting the cache
     */
    private void deleteCache(Cache cache) throws IOException {
		if (cache.getVolume() != null) {
			deleteVolume(cache.getVolume().getVolumeName());
		}
		if (cache.getBind() != null) {
			deleteBind(cache.getBind().getSource());
		}
	}

	/**
     * Deletes a volume with the specified name.
     * 
     * @param name the name of the volume to be deleted
     * @throws IOException if an I/O error occurs while deleting the volume
     */
    private void deleteVolume(VolumeName name) throws IOException {
		this.docker.volume().delete(name, true);
	}

	/**
     * Deletes the bind mount directory specified by the given source path.
     * 
     * @param source the path of the bind mount directory to be deleted
     * @throws IllegalStateException if an error occurs while cleaning the bind mount directory
     */
    private void deleteBind(String source) {
		try {
			FileSystemUtils.deleteRecursively(Path.of(source));
		}
		catch (IOException ex) {
			throw new IllegalStateException("Error cleaning bind mount directory '" + source + "'", ex);
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
