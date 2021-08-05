/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.maven;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;

import org.springframework.boot.buildpack.platform.build.BuildRequest;
import org.springframework.boot.buildpack.platform.build.BuildpackReference;
import org.springframework.boot.buildpack.platform.build.PullPolicy;
import org.springframework.boot.buildpack.platform.docker.type.Binding;
import org.springframework.boot.buildpack.platform.docker.type.ImageName;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.io.Owner;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Image configuration options.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 */
public class Image {

	String name;

	String builder;

	String runImage;

	Map<String, String> env;

	Boolean cleanCache;

	boolean verboseLogging;

	PullPolicy pullPolicy;

	Boolean publish;

	List<String> buildpacks;

	List<String> bindings;

	/**
	 * The name of the created image.
	 * @return the image name
	 */
	public String getName() {
		return this.name;
	}

	void setName(String name) {
		this.name = name;
	}

	/**
	 * The name of the builder image to use to create the image.
	 * @return the builder image name
	 */
	public String getBuilder() {
		return this.builder;
	}

	void setBuilder(String builder) {
		this.builder = builder;
	}

	/**
	 * The name of the run image to use to create the image.
	 * @return the builder image name
	 */
	public String getRunImage() {
		return this.runImage;
	}

	void setRunImage(String runImage) {
		this.runImage = runImage;
	}

	/**
	 * Environment properties that should be passed to the builder.
	 * @return the environment properties
	 */
	public Map<String, String> getEnv() {
		return this.env;
	}

	/**
	 * If the cache should be cleaned before building.
	 * @return {@code true} if the cache should be cleaned
	 */
	public Boolean getCleanCache() {
		return this.cleanCache;
	}

	void setCleanCache(Boolean cleanCache) {
		this.cleanCache = cleanCache;
	}

	/**
	 * If verbose logging is required.
	 * @return {@code true} for verbose logging
	 */
	public boolean isVerboseLogging() {
		return this.verboseLogging;
	}

	/**
	 * If images should be pulled from a remote repository during image build.
	 * @return the pull policy
	 */
	public PullPolicy getPullPolicy() {
		return this.pullPolicy;
	}

	void setPullPolicy(PullPolicy pullPolicy) {
		this.pullPolicy = pullPolicy;
	}

	/**
	 * If the built image should be pushed to a registry.
	 * @return {@code true} if the image should be published
	 */
	public Boolean getPublish() {
		return this.publish;
	}

	void setPublish(Boolean publish) {
		this.publish = publish;
	}

	BuildRequest getBuildRequest(Artifact artifact, Function<Owner, TarArchive> applicationContent) {
		return customize(BuildRequest.of(getOrDeduceName(artifact), applicationContent));
	}

	private ImageReference getOrDeduceName(Artifact artifact) {
		if (StringUtils.hasText(this.name)) {
			return ImageReference.of(this.name);
		}
		ImageName imageName = ImageName.of(artifact.getArtifactId());
		return ImageReference.of(imageName, artifact.getVersion());
	}

	private BuildRequest customize(BuildRequest request) {
		if (StringUtils.hasText(this.builder)) {
			request = request.withBuilder(ImageReference.of(this.builder));
		}
		if (StringUtils.hasText(this.runImage)) {
			request = request.withRunImage(ImageReference.of(this.runImage));
		}
		if (this.env != null && !this.env.isEmpty()) {
			request = request.withEnv(this.env);
		}
		if (this.cleanCache != null) {
			request = request.withCleanCache(this.cleanCache);
		}
		request = request.withVerboseLogging(this.verboseLogging);
		if (this.pullPolicy != null) {
			request = request.withPullPolicy(this.pullPolicy);
		}
		if (this.publish != null) {
			request = request.withPublish(this.publish);
		}
		if (!CollectionUtils.isEmpty(this.buildpacks)) {
			request = request
					.withBuildpacks(this.buildpacks.stream().map(BuildpackReference::of).collect(Collectors.toList()));
		}
		if (!CollectionUtils.isEmpty(this.bindings)) {
			request = request.withBindings(this.bindings.stream().map(Binding::of).collect(Collectors.toList()));
		}
		return request;
	}

}
