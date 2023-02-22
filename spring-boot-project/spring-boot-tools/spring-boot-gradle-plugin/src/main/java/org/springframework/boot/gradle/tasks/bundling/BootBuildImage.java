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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.gradle.work.DisableCachingByDefault;

import org.springframework.boot.buildpack.platform.build.BuildRequest;
import org.springframework.boot.buildpack.platform.build.Builder;
import org.springframework.boot.buildpack.platform.build.BuildpackReference;
import org.springframework.boot.buildpack.platform.build.Creator;
import org.springframework.boot.buildpack.platform.build.PullPolicy;
import org.springframework.boot.buildpack.platform.docker.transport.DockerEngineException;
import org.springframework.boot.buildpack.platform.docker.type.Binding;
import org.springframework.boot.buildpack.platform.docker.type.ImageName;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.io.ZipFileTarArchive;
import org.springframework.boot.gradle.util.VersionExtractor;
import org.springframework.util.StringUtils;

/**
 * A {@link Task} for bundling an application into an OCI image using a
 * <a href="https://buildpacks.io">buildpack</a>.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Rafael Ceccone
 * @author Jeroen Meijer
 * @author Julian Liebig
 * @since 2.3.0
 */
@DisableCachingByDefault
public abstract class BootBuildImage extends DefaultTask {

	private final Property<PullPolicy> pullPolicy;

	private final String projectName;

	private final CacheSpec buildCache;

	private final CacheSpec launchCache;

	private final DockerSpec docker;

	public BootBuildImage() {
		this.projectName = getProject().getName();
		Project project = getProject();
		Property<String> projectVersion = project.getObjects()
			.property(String.class)
			.convention(project.provider(() -> project.getVersion().toString()));
		getImageName().convention(project.provider(() -> {
			ImageName imageName = ImageName.of(this.projectName);
			if ("unspecified".equals(projectVersion.get())) {
				return ImageReference.of(imageName).toString();
			}
			return ImageReference.of(imageName, projectVersion.get()).toString();
		}));
		getCleanCache().convention(false);
		getVerboseLogging().convention(false);
		getPublish().convention(false);
		this.buildCache = getProject().getObjects().newInstance(CacheSpec.class);
		this.launchCache = getProject().getObjects().newInstance(CacheSpec.class);
		this.docker = getProject().getObjects().newInstance(DockerSpec.class);
		this.pullPolicy = getProject().getObjects().property(PullPolicy.class);
	}

	/**
	 * Returns the property for the archive file from which the image will be built.
	 * @return the archive file property
	 */
	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract RegularFileProperty getArchiveFile();

	/**
	 * Returns the name of the image that will be built. When {@code null}, the name will
	 * be derived from the {@link Project Project's} {@link Project#getName() name} and
	 * {@link Project#getVersion version}.
	 * @return name of the image
	 */
	@Input
	@Optional
	@Option(option = "imageName", description = "The name of the image to generate")
	public abstract Property<String> getImageName();

	/**
	 * Returns the builder that will be used to build the image. When {@code null}, the
	 * default builder will be used.
	 * @return the builder
	 */
	@Input
	@Optional
	@Option(option = "builder", description = "The name of the builder image to use")
	public abstract Property<String> getBuilder();

	/**
	 * Returns the run image that will be included in the built image. When {@code null},
	 * the run image bundled with the builder will be used.
	 * @return the run image
	 */
	@Input
	@Optional
	@Option(option = "runImage", description = "The name of the run image to use")
	public abstract Property<String> getRunImage();

	/**
	 * Returns the environment that will be used when building the image.
	 * @return the environment
	 */
	@Input
	public abstract MapProperty<String, String> getEnvironment();

	/**
	 * Returns whether caches should be cleaned before packaging.
	 * @return whether caches should be cleaned
	 * @since 3.0.0
	 */
	@Input
	@Option(option = "cleanCache", description = "Clean caches before packaging")
	public abstract Property<Boolean> getCleanCache();

	/**
	 * Whether verbose logging should be enabled while building the image.
	 * @return whether verbose logging should be enabled
	 * @since 3.0.0
	 */
	@Input
	public abstract Property<Boolean> getVerboseLogging();

	/**
	 * Returns image pull policy that will be used when building the image.
	 * @return whether images should be pulled
	 */
	@Input
	@Optional
	@Option(option = "pullPolicy", description = "The image pull policy")
	public Property<PullPolicy> getPullPolicy() {
		return this.pullPolicy;
	}

	/**
	 * Sets image pull policy that will be used when building the image.
	 * @param pullPolicy the pull policy to use
	 */
	public void setPullPolicy(String pullPolicy) {
		getPullPolicy().set(PullPolicy.valueOf(pullPolicy));
	}

	/**
	 * Whether the built image should be pushed to a registry.
	 * @return whether the built image should be pushed
	 * @since 3.0.0
	 */
	@Input
	@Option(option = "publishImage", description = "Publish the built image to a registry")
	public abstract Property<Boolean> getPublish();

	/**
	 * Returns the buildpacks that will be used when building the image.
	 * @return the buildpack references
	 */
	@Input
	@Optional
	public abstract ListProperty<String> getBuildpacks();

	/**
	 * Returns the volume bindings that will be mounted to the container when building the
	 * image.
	 * @return the bindings
	 */
	@Input
	@Optional
	public abstract ListProperty<String> getBindings();

	/**
	 * Returns the tags that will be created for the built image.
	 * @return the tags
	 */
	@Input
	@Optional
	public abstract ListProperty<String> getTags();

	/**
	 * Returns the network the build container will connect to.
	 * @return the network
	 */
	@Input
	@Optional
	@Option(option = "network", description = "Connect detect and build containers to network")
	public abstract Property<String> getNetwork();

	/**
	 * Returns the build cache that will be used when building the image.
	 * @return the cache
	 */
	@Nested
	@Optional
	public CacheSpec getBuildCache() {
		return this.buildCache;
	}

	/**
	 * Customizes the {@link CacheSpec} for the build cache using the given
	 * {@code action}.
	 * @param action the action
	 */
	public void buildCache(Action<CacheSpec> action) {
		action.execute(this.buildCache);
	}

	/**
	 * Returns the launch cache that will be used when building the image.
	 * @return the cache
	 */
	@Nested
	@Optional
	public CacheSpec getLaunchCache() {
		return this.launchCache;
	}

	/**
	 * Customizes the {@link CacheSpec} for the launch cache using the given
	 * {@code action}.
	 * @param action the action
	 */
	public void launchCache(Action<CacheSpec> action) {
		action.execute(this.launchCache);
	}

	/**
	 * Returns the Docker configuration the builder will use.
	 * @return docker configuration.
	 * @since 2.4.0
	 */
	@Nested
	public DockerSpec getDocker() {
		return this.docker;
	}

	/**
	 * Configures the Docker connection using the given {@code action}.
	 * @param action the action to apply
	 * @since 2.4.0
	 */
	public void docker(Action<DockerSpec> action) {
		action.execute(this.docker);
	}

	@TaskAction
	void buildImage() throws DockerEngineException, IOException {
		Builder builder = new Builder(this.docker.asDockerConfiguration());
		BuildRequest request = createRequest();
		builder.build(request);
	}

	BuildRequest createRequest() {
		return customize(BuildRequest.of(getImageName().map(ImageReference::of).get(),
				(owner) -> new ZipFileTarArchive(getArchiveFile().get().getAsFile(), owner)));
	}

	private BuildRequest customize(BuildRequest request) {
		request = customizeBuilder(request);
		request = customizeRunImage(request);
		request = customizeEnvironment(request);
		request = customizeCreator(request);
		request = request.withCleanCache(getCleanCache().get());
		request = request.withVerboseLogging(getVerboseLogging().get());
		request = customizePullPolicy(request);
		request = customizePublish(request);
		request = customizeBuildpacks(request);
		request = customizeBindings(request);
		request = customizeTags(request);
		request = customizeCaches(request);
		request = request.withNetwork(getNetwork().getOrNull());
		return request;
	}

	private BuildRequest customizeBuilder(BuildRequest request) {
		String builder = getBuilder().getOrNull();
		if (StringUtils.hasText(builder)) {
			return request.withBuilder(ImageReference.of(builder));
		}
		return request;
	}

	private BuildRequest customizeRunImage(BuildRequest request) {
		String runImage = getRunImage().getOrNull();
		if (StringUtils.hasText(runImage)) {
			return request.withRunImage(ImageReference.of(runImage));
		}
		return request;
	}

	private BuildRequest customizeEnvironment(BuildRequest request) {
		Map<String, String> environment = getEnvironment().getOrNull();
		if (environment != null && !environment.isEmpty()) {
			request = request.withEnv(environment);
		}
		return request;
	}

	private BuildRequest customizeCreator(BuildRequest request) {
		String springBootVersion = VersionExtractor.forClass(BootBuildImage.class);
		if (StringUtils.hasText(springBootVersion)) {
			return request.withCreator(Creator.withVersion(springBootVersion));
		}
		return request;
	}

	private BuildRequest customizePullPolicy(BuildRequest request) {
		PullPolicy pullPolicy = getPullPolicy().getOrNull();
		if (pullPolicy != null) {
			request = request.withPullPolicy(pullPolicy);
		}
		return request;
	}

	private BuildRequest customizePublish(BuildRequest request) {
		request = request.withPublish(getPublish().get());
		return request;
	}

	private BuildRequest customizeBuildpacks(BuildRequest request) {
		List<String> buildpacks = getBuildpacks().getOrNull();
		if (buildpacks != null && !buildpacks.isEmpty()) {
			return request.withBuildpacks(buildpacks.stream().map(BuildpackReference::of).toList());
		}
		return request;
	}

	private BuildRequest customizeBindings(BuildRequest request) {
		List<String> bindings = getBindings().getOrNull();
		if (bindings != null && !bindings.isEmpty()) {
			return request.withBindings(bindings.stream().map(Binding::of).toList());
		}
		return request;
	}

	private BuildRequest customizeTags(BuildRequest request) {
		List<String> tags = getTags().getOrNull();
		if (tags != null && !tags.isEmpty()) {
			return request.withTags(tags.stream().map(ImageReference::of).toList());
		}
		return request;
	}

	private BuildRequest customizeCaches(BuildRequest request) {
		if (this.buildCache.asCache() != null) {
			request = request.withBuildCache(this.buildCache.asCache());
		}
		if (this.launchCache.asCache() != null) {
			request = request.withLaunchCache(this.launchCache.asCache());
		}
		return request;
	}

}
