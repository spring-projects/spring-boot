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

package org.springframework.boot.maven;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
 * @author Jeroen Meijer
 * @author Rafael Ceccone
 * @author Julian Liebig
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

	String network;

	List<String> tags;

	CacheInfo buildWorkspace;

	CacheInfo buildCache;

	CacheInfo launchCache;

	String createdDate;

	String applicationDirectory;

	List<String> securityOptions;

	/**
	 * The name of the created image.
	 * @return the image name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the name of the image.
	 * @param name the name to be set for the image
	 */
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

	/**
	 * Sets the builder of the Image.
	 * @param builder the name of the builder to be set
	 */
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

	/**
	 * Sets the run image for the Image object.
	 * @param runImage the path or name of the run image
	 */
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

	/**
	 * Sets the value indicating whether the cache should be cleaned.
	 * @param cleanCache the value indicating whether the cache should be cleaned
	 */
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

	/**
	 * Sets the pull policy for the image.
	 * @param pullPolicy the pull policy to be set
	 */
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

	/**
	 * Sets the publish status of the image.
	 * @param publish the publish status to be set
	 */
	void setPublish(Boolean publish) {
		this.publish = publish;
	}

	/**
	 * Returns the network the build container will connect to.
	 * @return the network
	 */
	public String getNetwork() {
		return this.network;
	}

	/**
	 * Sets the network of the image.
	 * @param network the network to set
	 */
	public void setNetwork(String network) {
		this.network = network;
	}

	/**
	 * Returns the created date for the image.
	 * @return the created date
	 */
	public String getCreatedDate() {
		return this.createdDate;
	}

	/**
	 * Sets the created date of the image.
	 * @param createdDate the created date to be set
	 */
	public void setCreatedDate(String createdDate) {
		this.createdDate = createdDate;
	}

	/**
	 * Returns the application content directory for the image.
	 * @return the application directory
	 */
	public String getApplicationDirectory() {
		return this.applicationDirectory;
	}

	/**
	 * Sets the application directory for the Image.
	 * @param applicationDirectory the directory path where the application is located
	 */
	public void setApplicationDirectory(String applicationDirectory) {
		this.applicationDirectory = applicationDirectory;
	}

	/**
	 * Returns a BuildRequest object based on the given artifact and application content.
	 * @param artifact the artifact to be used in the build request
	 * @param applicationContent a function that takes an Owner object and returns a
	 * TarArchive object representing the application content
	 * @return a BuildRequest object customized based on the artifact and application
	 * content
	 */
	BuildRequest getBuildRequest(Artifact artifact, Function<Owner, TarArchive> applicationContent) {
		return customize(BuildRequest.of(getOrDeduceName(artifact), applicationContent));
	}

	/**
	 * Returns the ImageReference based on the provided Artifact. If the name is not
	 * empty, it returns the ImageReference with the provided name. Otherwise, it deduces
	 * the name from the Artifact's artifactId and creates the ImageReference with the
	 * deduced name and version.
	 * @param artifact the Artifact object used to generate the ImageReference
	 * @return the ImageReference based on the provided Artifact
	 */
	private ImageReference getOrDeduceName(Artifact artifact) {
		if (StringUtils.hasText(this.name)) {
			return ImageReference.of(this.name);
		}
		ImageName imageName = ImageName.of(artifact.getArtifactId());
		return ImageReference.of(imageName, artifact.getVersion());
	}

	/**
	 * Customizes the given BuildRequest object based on the values of the properties in
	 * this Image class.
	 * @param request the BuildRequest object to be customized
	 * @return the customized BuildRequest object
	 */
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
			request = request.withBuildpacks(this.buildpacks.stream().map(BuildpackReference::of).toList());
		}
		if (!CollectionUtils.isEmpty(this.bindings)) {
			request = request.withBindings(this.bindings.stream().map(Binding::of).toList());
		}
		request = request.withNetwork(this.network);
		if (!CollectionUtils.isEmpty(this.tags)) {
			request = request.withTags(this.tags.stream().map(ImageReference::of).toList());
		}
		if (this.buildWorkspace != null) {
			request = request.withBuildWorkspace(this.buildWorkspace.asCache());
		}
		if (this.buildCache != null) {
			request = request.withBuildCache(this.buildCache.asCache());
		}
		if (this.launchCache != null) {
			request = request.withLaunchCache(this.launchCache.asCache());
		}
		if (StringUtils.hasText(this.createdDate)) {
			request = request.withCreatedDate(this.createdDate);
		}
		if (StringUtils.hasText(this.applicationDirectory)) {
			request = request.withApplicationDirectory(this.applicationDirectory);
		}
		if (this.securityOptions != null) {
			request = request.withSecurityOptions(this.securityOptions);
		}
		return request;
	}

}
