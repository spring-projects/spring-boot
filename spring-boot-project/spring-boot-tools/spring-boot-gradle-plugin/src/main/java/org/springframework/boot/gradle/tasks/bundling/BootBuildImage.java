/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
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
public class BootBuildImage extends DefaultTask {

	private static final String BUILDPACK_JVM_VERSION_KEY = "BP_JVM_VERSION";

	private final String projectName;

	private final Property<String> projectVersion;

	private RegularFileProperty archiveFile;

	private Property<JavaVersion> targetJavaVersion;

	private String imageName;

	private String builder;

	private String runImage;

	private Map<String, String> environment = new HashMap<>();

	private boolean cleanCache;

	private boolean verboseLogging;

	private PullPolicy pullPolicy;

	private boolean publish;

	private final ListProperty<String> buildpacks;

	private final ListProperty<String> bindings;

	private String network;

	private final ListProperty<String> tags;

	private final CacheSpec buildCache;

	private final CacheSpec launchCache;

	private final DockerSpec docker;

	public BootBuildImage() {
		this.archiveFile = getProject().getObjects().fileProperty();
		this.targetJavaVersion = getProject().getObjects().property(JavaVersion.class);
		this.projectName = getProject().getName();
		this.projectVersion = getProject().getObjects().property(String.class);
		Project project = getProject();
		this.projectVersion.set(getProject().provider(() -> project.getVersion().toString()));
		this.buildpacks = getProject().getObjects().listProperty(String.class);
		this.bindings = getProject().getObjects().listProperty(String.class);
		this.tags = getProject().getObjects().listProperty(String.class);
		this.buildCache = getProject().getObjects().newInstance(CacheSpec.class);
		this.launchCache = getProject().getObjects().newInstance(CacheSpec.class);
		this.docker = getProject().getObjects().newInstance(DockerSpec.class);
	}

	/**
	 * Returns the property for the archive file from which the image will be built.
	 * @return the archive file property
	 */
	@Input
	public RegularFileProperty getArchiveFile() {
		return this.archiveFile;
	}

	/**
	 * Returns the target Java version of the project (e.g. as provided by the
	 * {@code targetCompatibility} build property).
	 * @return the target Java version
	 */
	@Input
	@Optional
	public Property<JavaVersion> getTargetJavaVersion() {
		return this.targetJavaVersion;
	}

	/**
	 * Returns the name of the image that will be built. When {@code null}, the name will
	 * be derived from the {@link Project Project's} {@link Project#getName() name} and
	 * {@link Project#getVersion version}.
	 * @return name of the image
	 */
	@Input
	@Optional
	public String getImageName() {
		return determineImageReference().toString();
	}

	/**
	 * Sets the name of the image that will be built.
	 * @param imageName name of the image
	 */
	@Option(option = "imageName", description = "The name of the image to generate")
	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	/**
	 * Returns the builder that will be used to build the image. When {@code null}, the
	 * default builder will be used.
	 * @return the builder
	 */
	@Input
	@Optional
	public String getBuilder() {
		return this.builder;
	}

	/**
	 * Sets the builder that will be used to build the image.
	 * @param builder the builder
	 */
	@Option(option = "builder", description = "The name of the builder image to use")
	public void setBuilder(String builder) {
		this.builder = builder;
	}

	/**
	 * Returns the run image that will be included in the built image. When {@code null},
	 * the run image bundled with the builder will be used.
	 * @return the run image
	 */
	@Input
	@Optional
	public String getRunImage() {
		return this.runImage;
	}

	/**
	 * Sets the run image that will be included in the built image.
	 * @param runImage the run image
	 */
	@Option(option = "runImage", description = "The name of the run image to use")
	public void setRunImage(String runImage) {
		this.runImage = runImage;
	}

	/**
	 * Returns the environment that will be used when building the image.
	 * @return the environment
	 */
	@Input
	public Map<String, String> getEnvironment() {
		return this.environment;
	}

	/**
	 * Sets the environment that will be used when building the image.
	 * @param environment the environment
	 */
	public void setEnvironment(Map<String, String> environment) {
		this.environment = environment;
	}

	/**
	 * Add an entry to the environment that will be used when building the image.
	 * @param name the name of the entry
	 * @param value the value of the entry
	 */
	public void environment(String name, String value) {
		this.environment.put(name, value);
	}

	/**
	 * Adds entries to the environment that will be used when building the image.
	 * @param entries the entries to add to the environment
	 */
	public void environment(Map<String, String> entries) {
		this.environment.putAll(entries);
	}

	/**
	 * Returns whether caches should be cleaned before packaging.
	 * @return whether caches should be cleaned
	 */
	@Input
	public boolean isCleanCache() {
		return this.cleanCache;
	}

	/**
	 * Sets whether caches should be cleaned before packaging.
	 * @param cleanCache {@code true} to clean the cache, otherwise {@code false}.
	 */
	@Option(option = "cleanCache", description = "Clean caches before packaging")
	public void setCleanCache(boolean cleanCache) {
		this.cleanCache = cleanCache;
	}

	/**
	 * Whether verbose logging should be enabled while building the image.
	 * @return whether verbose logging should be enabled
	 */
	@Input
	public boolean isVerboseLogging() {
		return this.verboseLogging;
	}

	/**
	 * Sets whether verbose logging should be enabled while building the image.
	 * @param verboseLogging {@code true} to enable verbose logging, otherwise
	 * {@code false}.
	 */
	public void setVerboseLogging(boolean verboseLogging) {
		this.verboseLogging = verboseLogging;
	}

	/**
	 * Returns image pull policy that will be used when building the image.
	 * @return whether images should be pulled
	 */
	@Input
	@Optional
	public PullPolicy getPullPolicy() {
		return this.pullPolicy;
	}

	/**
	 * Sets image pull policy that will be used when building the image.
	 * @param pullPolicy image pull policy {@link PullPolicy}
	 */
	@Option(option = "pullPolicy", description = "The image pull policy")
	public void setPullPolicy(PullPolicy pullPolicy) {
		this.pullPolicy = pullPolicy;
	}

	/**
	 * Whether the built image should be pushed to a registry.
	 * @return whether the built image should be pushed
	 */
	@Input
	public boolean isPublish() {
		return this.publish;
	}

	/**
	 * Sets whether the built image should be pushed to a registry.
	 * @param publish {@code true} the push the built image to a registry. {@code false}.
	 */
	@Option(option = "publishImage", description = "Publish the built image to a registry")
	public void setPublish(boolean publish) {
		this.publish = publish;
	}

	/**
	 * Returns the buildpacks that will be used when building the image.
	 * @return the buildpack references
	 */
	@Input
	@Optional
	public List<String> getBuildpacks() {
		return this.buildpacks.getOrNull();
	}

	/**
	 * Sets the buildpacks that will be used when building the image.
	 * @param buildpacks the buildpack references
	 */
	public void setBuildpacks(List<String> buildpacks) {
		this.buildpacks.set(buildpacks);
	}

	/**
	 * Add an entry to the buildpacks that will be used when building the image.
	 * @param buildpack the buildpack reference
	 */
	public void buildpack(String buildpack) {
		this.buildpacks.add(buildpack);
	}

	/**
	 * Adds entries to the buildpacks that will be used when building the image.
	 * @param buildpacks the buildpack references
	 */
	public void buildpacks(List<String> buildpacks) {
		this.buildpacks.addAll(buildpacks);
	}

	/**
	 * Returns the volume bindings that will be mounted to the container when building the
	 * image.
	 * @return the bindings
	 */
	@Input
	@Optional
	public List<String> getBindings() {
		return this.bindings.getOrNull();
	}

	/**
	 * Sets the volume bindings that will be mounted to the container when building the
	 * image.
	 * @param bindings the bindings
	 */
	public void setBindings(List<String> bindings) {
		this.bindings.set(bindings);
	}

	/**
	 * Add an entry to the volume bindings that will be mounted to the container when
	 * building the image.
	 * @param binding the binding
	 */
	public void binding(String binding) {
		this.bindings.add(binding);
	}

	/**
	 * Add entries to the volume bindings that will be mounted to the container when
	 * building the image.
	 * @param bindings the bindings
	 */
	public void bindings(List<String> bindings) {
		this.bindings.addAll(bindings);
	}

	/**
	 * Returns the tags that will be created for the built image.
	 * @return the tags
	 */
	@Input
	@Optional
	public List<String> getTags() {
		return this.tags.getOrNull();
	}

	/**
	 * Sets the tags that will be created for the built image.
	 * @param tags the tags
	 */
	public void setTags(List<String> tags) {
		this.tags.set(tags);
	}

	/**
	 * Add an entry to the tags that will be created for the built image.
	 * @param tag the tag
	 */
	public void tag(String tag) {
		this.tags.add(tag);
	}

	/**
	 * Add entries to the tags that will be created for the built image.
	 * @param tags the tags
	 */
	public void tags(List<String> tags) {
		this.tags.addAll(tags);
	}

	/**
	 * Returns the network the build container will connect to.
	 * @return the network
	 */
	@Input
	@Optional
	public String getNetwork() {
		return this.network;
	}

	/**
	 * Sets the network the build container will connect to.
	 * @param network the network
	 */
	@Option(option = "network", description = "Connect detect and build containers to network")
	public void setNetwork(String network) {
		this.network = network;
	}

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
		return customize(BuildRequest.of(determineImageReference(),
				(owner) -> new ZipFileTarArchive(this.archiveFile.get().getAsFile(), owner)));
	}

	private ImageReference determineImageReference() {
		if (StringUtils.hasText(this.imageName)) {
			return ImageReference.of(this.imageName);
		}
		ImageName imageName = ImageName.of(this.projectName);
		if ("unspecified".equals(this.projectVersion.get())) {
			return ImageReference.of(imageName);
		}
		return ImageReference.of(imageName, this.projectVersion.get());
	}

	private BuildRequest customize(BuildRequest request) {
		request = customizeBuilder(request);
		request = customizeRunImage(request);
		request = customizeEnvironment(request);
		request = customizeCreator(request);
		request = request.withCleanCache(this.cleanCache);
		request = request.withVerboseLogging(this.verboseLogging);
		request = customizePullPolicy(request);
		request = customizePublish(request);
		request = customizeBuildpacks(request);
		request = customizeBindings(request);
		request = customizeTags(request);
		request = customizeCaches(request);
		request = request.withNetwork(this.network);
		return request;
	}

	private BuildRequest customizeBuilder(BuildRequest request) {
		if (StringUtils.hasText(this.builder)) {
			return request.withBuilder(ImageReference.of(this.builder));
		}
		return request;
	}

	private BuildRequest customizeRunImage(BuildRequest request) {
		if (StringUtils.hasText(this.runImage)) {
			return request.withRunImage(ImageReference.of(this.runImage));
		}
		return request;
	}

	private BuildRequest customizeEnvironment(BuildRequest request) {
		if (this.environment != null && !this.environment.isEmpty()) {
			request = request.withEnv(this.environment);
		}
		if (this.targetJavaVersion.isPresent() && !request.getEnv().containsKey(BUILDPACK_JVM_VERSION_KEY)) {
			request = request.withEnv(BUILDPACK_JVM_VERSION_KEY, translateTargetJavaVersion());
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
		if (this.pullPolicy != null) {
			request = request.withPullPolicy(this.pullPolicy);
		}
		return request;
	}

	private BuildRequest customizePublish(BuildRequest request) {
		request = request.withPublish(this.publish);
		return request;
	}

	private BuildRequest customizeBuildpacks(BuildRequest request) {
		List<String> buildpacks = this.buildpacks.getOrNull();
		if (buildpacks != null && !buildpacks.isEmpty()) {
			return request.withBuildpacks(buildpacks.stream().map(BuildpackReference::of).collect(Collectors.toList()));
		}
		return request;
	}

	private BuildRequest customizeBindings(BuildRequest request) {
		List<String> bindings = this.bindings.getOrNull();
		if (bindings != null && !bindings.isEmpty()) {
			return request.withBindings(bindings.stream().map(Binding::of).collect(Collectors.toList()));
		}
		return request;
	}

	private BuildRequest customizeTags(BuildRequest request) {
		List<String> tags = this.tags.getOrNull();
		if (tags != null && !tags.isEmpty()) {
			return request.withTags(tags.stream().map(ImageReference::of).collect(Collectors.toList()));
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

	private String translateTargetJavaVersion() {
		return this.targetJavaVersion.get().getMajorVersion() + ".*";
	}

}
