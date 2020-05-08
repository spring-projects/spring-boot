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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import org.springframework.boot.buildpack.platform.build.BuildRequest;
import org.springframework.boot.buildpack.platform.build.Builder;
import org.springframework.boot.buildpack.platform.build.Creator;
import org.springframework.boot.buildpack.platform.docker.transport.DockerEngineException;
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
 * @since 2.3.0
 */
public class BootBuildImage extends DefaultTask {

	private static final String BUILDPACK_JVM_VERSION_KEY = "BP_JVM_VERSION";

	private RegularFileProperty jar;

	private Property<JavaVersion> targetJavaVersion;

	private String imageName;

	private String builder;

	private Map<String, String> environment = new HashMap<>();

	private boolean cleanCache;

	private boolean verboseLogging;

	public BootBuildImage() {
		this.jar = getProject().getObjects().fileProperty();
		this.targetJavaVersion = getProject().getObjects().property(JavaVersion.class);
	}

	/**
	 * Returns the property for the jar file from which the image will be built.
	 * @return the jar property
	 */
	@Input
	public RegularFileProperty getJar() {
		return this.jar;
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
		return this.imageName;
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

	@TaskAction
	void buildImage() throws DockerEngineException, IOException {
		Builder builder = new Builder();
		BuildRequest request = createRequest();
		builder.build(request);
	}

	BuildRequest createRequest() {
		return customize(BuildRequest.of(determineImageReference(),
				(owner) -> new ZipFileTarArchive(this.jar.get().getAsFile(), owner)));
	}

	private ImageReference determineImageReference() {
		if (StringUtils.hasText(this.imageName)) {
			return ImageReference.of(this.imageName);
		}
		ImageName imageName = ImageName.of(getProject().getName());
		String version = getProject().getVersion().toString();
		if ("unspecified".equals(version)) {
			return ImageReference.of(imageName);
		}
		return ImageReference.of(imageName, version);
	}

	private BuildRequest customize(BuildRequest request) {
		request = customizeBuilder(request);
		request = customizeEnvironment(request);
		request = customizeCreator(request);
		request = request.withCleanCache(this.cleanCache);
		request = request.withVerboseLogging(this.verboseLogging);
		return request;
	}

	private BuildRequest customizeBuilder(BuildRequest request) {
		if (StringUtils.hasText(this.builder)) {
			return request.withBuilder(ImageReference.of(this.builder));
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

	private String translateTargetJavaVersion() {
		return this.targetJavaVersion.get().getMajorVersion() + ".*";
	}

}
