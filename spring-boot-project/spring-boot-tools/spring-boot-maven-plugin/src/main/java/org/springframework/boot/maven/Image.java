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

package org.springframework.boot.maven;

import java.util.Map;
import java.util.function.Function;

import org.apache.maven.artifact.Artifact;

import org.springframework.boot.buildpack.platform.build.BuildRequest;
import org.springframework.boot.buildpack.platform.build.PullPolicy;
import org.springframework.boot.buildpack.platform.docker.type.ImageName;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.io.Owner;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.util.StringUtils;

/**
 * Image configuration options.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 */
public class Image {

	/**
	 * The name of the created image.
	 */
	String name;

	/**
	 * The builder used to create the image.
	 */
	String builder;

	/**
	 * The run image used to launch the built image.
	 */
	String runImage;

	/**
	 * Environment properties that should be passed to the builder.
	 */
	Map<String, String> env;

	/**
	 * If the cache should be cleaned before building.
	 */
	boolean cleanCache;

	/**
	 * If verbose logging is required.
	 */
	boolean verboseLogging;

	/**
	 * If images should be pulled from a remote repository during image build.
	 */
	PullPolicy pullPolicy;

	void setName(String name) {
		this.name = name;
	}

	void setBuilder(String builder) {
		this.builder = builder;
	}

	void setRunImage(String runImage) {
		this.runImage = runImage;
	}

	public void setPullPolicy(PullPolicy pullPolicy) {
		this.pullPolicy = pullPolicy;
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
		request = request.withCleanCache(this.cleanCache);
		request = request.withVerboseLogging(this.verboseLogging);
		if (this.pullPolicy != null) {
			request = request.withPullPolicy(this.pullPolicy);
		}
		return request;
	}

}
