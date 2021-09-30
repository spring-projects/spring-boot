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

package org.springframework.boot.buildpack.platform.build;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.boot.buildpack.platform.build.BuilderMetadata.Stack;
import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.TotalProgressEvent;
import org.springframework.boot.buildpack.platform.docker.TotalProgressPullListener;
import org.springframework.boot.buildpack.platform.docker.TotalProgressPushListener;
import org.springframework.boot.buildpack.platform.docker.UpdateListener;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration;
import org.springframework.boot.buildpack.platform.docker.transport.DockerEngineException;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.io.IOBiConsumer;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Central API for running buildpack operations.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Andrey Shlykov
 * @author Rafael Ceccone
 * @since 2.3.0
 */
public class Builder {

	private final BuildLog log;

	private final DockerApi docker;

	private final DockerConfiguration dockerConfiguration;

	/**
	 * Create a new builder instance.
	 */
	public Builder() {
		this(BuildLog.toSystemOut());
	}

	/**
	 * Create a new builder instance.
	 * @param dockerConfiguration the docker configuration
	 * @since 2.4.0
	 */
	public Builder(DockerConfiguration dockerConfiguration) {
		this(BuildLog.toSystemOut(), dockerConfiguration);
	}

	/**
	 * Create a new builder instance.
	 * @param log a logger used to record output
	 */
	public Builder(BuildLog log) {
		this(log, new DockerApi(), null);
	}

	/**
	 * Create a new builder instance.
	 * @param log a logger used to record output
	 * @param dockerConfiguration the docker configuration
	 * @since 2.4.0
	 */
	public Builder(BuildLog log, DockerConfiguration dockerConfiguration) {
		this(log, new DockerApi(dockerConfiguration), dockerConfiguration);
	}

	Builder(BuildLog log, DockerApi docker, DockerConfiguration dockerConfiguration) {
		Assert.notNull(log, "Log must not be null");
		this.log = log;
		this.docker = docker;
		this.dockerConfiguration = dockerConfiguration;
	}

	public void build(BuildRequest request) throws DockerEngineException, IOException {
		Assert.notNull(request, "Request must not be null");
		this.log.start(request);
		String domain = request.getBuilder().getDomain();
		PullPolicy pullPolicy = request.getPullPolicy();
		ImageFetcher imageFetcher = new ImageFetcher(domain, getBuilderAuthHeader(), pullPolicy);
		Image builderImage = imageFetcher.fetchImage(ImageType.BUILDER, request.getBuilder());
		BuilderMetadata builderMetadata = BuilderMetadata.fromImage(builderImage);
		request = withRunImageIfNeeded(request, builderMetadata.getStack());
		Image runImage = imageFetcher.fetchImage(ImageType.RUNNER, request.getRunImage());
		assertStackIdsMatch(runImage, builderImage);
		BuildOwner buildOwner = BuildOwner.fromEnv(builderImage.getConfig().getEnv());
		Buildpacks buildpacks = getBuildpacks(request, imageFetcher, builderMetadata);
		EphemeralBuilder ephemeralBuilder = new EphemeralBuilder(buildOwner, builderImage, request.getName(),
				builderMetadata, request.getCreator(), request.getEnv(), buildpacks);
		this.docker.image().load(ephemeralBuilder.getArchive(), UpdateListener.none());
		try {
			executeLifecycle(request, ephemeralBuilder);
			tagImage(request.getName(), request.getTags());
			if (request.isPublish()) {
				pushImages(request.getName(), request.getTags());
			}
		}
		finally {
			this.docker.image().remove(ephemeralBuilder.getName(), true);
		}
	}

	private BuildRequest withRunImageIfNeeded(BuildRequest request, Stack builderStack) {
		if (request.getRunImage() != null) {
			return request;
		}
		return request.withRunImage(getRunImageReferenceForStack(builderStack));
	}

	private ImageReference getRunImageReferenceForStack(Stack stack) {
		String name = stack.getRunImage().getImage();
		Assert.state(StringUtils.hasText(name), "Run image must be specified in the builder image stack");
		return ImageReference.of(name).inTaggedOrDigestForm();
	}

	private void assertStackIdsMatch(Image runImage, Image builderImage) {
		StackId runImageStackId = StackId.fromImage(runImage);
		StackId builderImageStackId = StackId.fromImage(builderImage);
		Assert.state(runImageStackId.equals(builderImageStackId), () -> "Run image stack '" + runImageStackId
				+ "' does not match builder stack '" + builderImageStackId + "'");
	}

	private Buildpacks getBuildpacks(BuildRequest request, ImageFetcher imageFetcher, BuilderMetadata builderMetadata) {
		BuildpackResolverContext resolverContext = new BuilderResolverContext(imageFetcher, builderMetadata);
		return BuildpackResolvers.resolveAll(resolverContext, request.getBuildpacks());
	}

	private void executeLifecycle(BuildRequest request, EphemeralBuilder builder) throws IOException {
		try (Lifecycle lifecycle = new Lifecycle(this.log, this.docker, request, builder)) {
			lifecycle.execute();
		}
	}

	private void tagImage(ImageReference sourceReference, List<ImageReference> tags) throws IOException {
		for (ImageReference tag : tags) {
			this.docker.image().tag(sourceReference, tag);
			this.log.taggedImage(tag);
		}
	}

	private void pushImages(ImageReference name, List<ImageReference> tags) throws IOException {
		pushImage(name);
		for (ImageReference tag : tags) {
			pushImage(tag);
		}
	}

	private void pushImage(ImageReference reference) throws IOException {
		Consumer<TotalProgressEvent> progressConsumer = this.log.pushingImage(reference);
		TotalProgressPushListener listener = new TotalProgressPushListener(progressConsumer);
		this.docker.image().push(reference, listener, getPublishAuthHeader());
		this.log.pushedImage(reference);
	}

	private String getBuilderAuthHeader() {
		return (this.dockerConfiguration != null && this.dockerConfiguration.getBuilderRegistryAuthentication() != null)
				? this.dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader() : null;
	}

	private String getPublishAuthHeader() {
		return (this.dockerConfiguration != null && this.dockerConfiguration.getPublishRegistryAuthentication() != null)
				? this.dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader() : null;
	}

	/**
	 * Internal utility class used to fetch images.
	 */
	private class ImageFetcher {

		private final String domain;

		private final String authHeader;

		private final PullPolicy pullPolicy;

		ImageFetcher(String domain, String authHeader, PullPolicy pullPolicy) {
			this.domain = domain;
			this.authHeader = authHeader;
			this.pullPolicy = pullPolicy;
		}

		Image fetchImage(ImageType type, ImageReference reference) throws IOException {
			Assert.notNull(type, "Type must not be null");
			Assert.notNull(reference, "Reference must not be null");
			Assert.state(this.authHeader == null || reference.getDomain().equals(this.domain),
					() -> String.format("%s '%s' must be pulled from the '%s' authenticated registry",
							StringUtils.capitalize(type.getDescription()), reference, this.domain));
			if (this.pullPolicy == PullPolicy.ALWAYS) {
				return pullImage(reference, type);
			}
			try {
				return Builder.this.docker.image().inspect(reference);
			}
			catch (DockerEngineException ex) {
				if (this.pullPolicy == PullPolicy.IF_NOT_PRESENT && ex.getStatusCode() == 404) {
					return pullImage(reference, type);
				}
				throw ex;
			}
		}

		private Image pullImage(ImageReference reference, ImageType imageType) throws IOException {
			TotalProgressPullListener listener = new TotalProgressPullListener(
					Builder.this.log.pullingImage(reference, imageType));
			Image image = Builder.this.docker.image().pull(reference, listener, this.authHeader);
			Builder.this.log.pulledImage(image, imageType);
			return image;
		}

	}

	/**
	 * {@link BuildpackResolverContext} implementation for the {@link Builder}.
	 */
	private class BuilderResolverContext implements BuildpackResolverContext {

		private final ImageFetcher imageFetcher;

		private final BuilderMetadata builderMetadata;

		BuilderResolverContext(ImageFetcher imageFetcher, BuilderMetadata builderMetadata) {
			this.imageFetcher = imageFetcher;
			this.builderMetadata = builderMetadata;
		}

		@Override
		public List<BuildpackMetadata> getBuildpackMetadata() {
			return this.builderMetadata.getBuildpacks();
		}

		@Override
		public Image fetchImage(ImageReference reference, ImageType imageType) throws IOException {
			return this.imageFetcher.fetchImage(imageType, reference);
		}

		@Override
		public void exportImageLayers(ImageReference reference, IOBiConsumer<String, TarArchive> exports)
				throws IOException {
			Builder.this.docker.image().exportLayers(reference, exports);
		}

	}

}
