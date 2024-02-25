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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.boot.buildpack.platform.build.BuilderMetadata.Stack;
import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.TotalProgressEvent;
import org.springframework.boot.buildpack.platform.docker.TotalProgressPullListener;
import org.springframework.boot.buildpack.platform.docker.TotalProgressPushListener;
import org.springframework.boot.buildpack.platform.docker.UpdateListener;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration;
import org.springframework.boot.buildpack.platform.docker.configuration.ResolvedDockerHost;
import org.springframework.boot.buildpack.platform.docker.transport.DockerEngineException;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.io.IOBiConsumer;
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
		this(log, new DockerApi((dockerConfiguration != null) ? dockerConfiguration.getHost() : null),
				dockerConfiguration);
	}

	/**
	 * Constructs a new Builder with the specified BuildLog, DockerApi, and
	 * DockerConfiguration.
	 * @param log the BuildLog to be used by the Builder (must not be null)
	 * @param docker the DockerApi to be used by the Builder
	 * @param dockerConfiguration the DockerConfiguration to be used by the Builder
	 * @throws IllegalArgumentException if the log is null
	 */
	Builder(BuildLog log, DockerApi docker, DockerConfiguration dockerConfiguration) {
		Assert.notNull(log, "Log must not be null");
		this.log = log;
		this.docker = docker;
		this.dockerConfiguration = dockerConfiguration;
	}

	/**
	 * Builds a Docker image based on the given build request.
	 * @param request the build request containing the necessary information for building
	 * the image
	 * @throws DockerEngineException if there is an error with the Docker engine
	 * @throws IOException if there is an I/O error
	 */
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
		BuildpackLayersMetadata buildpackLayersMetadata = BuildpackLayersMetadata.fromImage(builderImage);
		Buildpacks buildpacks = getBuildpacks(request, imageFetcher, builderMetadata, buildpackLayersMetadata);
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

	/**
	 * Checks if the given BuildRequest has a run image specified. If not, it sets the run
	 * image based on the provided builderStack.
	 * @param request The BuildRequest to be checked and modified if necessary.
	 * @param builderStack The Stack object used to determine the run image reference.
	 * @return The modified BuildRequest object with the run image set, if necessary.
	 */
	private BuildRequest withRunImageIfNeeded(BuildRequest request, Stack builderStack) {
		if (request.getRunImage() != null) {
			return request;
		}
		return request.withRunImage(getRunImageReferenceForStack(builderStack));
	}

	/**
	 * Returns the image reference for the run image of the given stack.
	 * @param stack the stack for which to get the run image reference
	 * @return the image reference for the run image
	 * @throws IllegalArgumentException if the run image is not specified in the builder
	 * image stack
	 */
	private ImageReference getRunImageReferenceForStack(Stack stack) {
		String name = stack.getRunImage().getImage();
		Assert.state(StringUtils.hasText(name), "Run image must be specified in the builder image stack");
		return ImageReference.of(name).inTaggedOrDigestForm();
	}

	/**
	 * Asserts that the stack IDs of the run image and builder image match.
	 * @param runImage The run image.
	 * @param builderImage The builder image.
	 * @throws IllegalStateException if the stack IDs do not match.
	 */
	private void assertStackIdsMatch(Image runImage, Image builderImage) {
		StackId runImageStackId = StackId.fromImage(runImage);
		StackId builderImageStackId = StackId.fromImage(builderImage);
		Assert.state(runImageStackId.equals(builderImageStackId), () -> "Run image stack '" + runImageStackId
				+ "' does not match builder stack '" + builderImageStackId + "'");
	}

	/**
	 * Retrieves the buildpacks for the given build request.
	 * @param request the build request containing the buildpacks
	 * @param imageFetcher the image fetcher used to fetch the builder image
	 * @param builderMetadata the metadata of the builder
	 * @param buildpackLayersMetadata the metadata of the buildpack layers
	 * @return the resolved buildpacks
	 */
	private Buildpacks getBuildpacks(BuildRequest request, ImageFetcher imageFetcher, BuilderMetadata builderMetadata,
			BuildpackLayersMetadata buildpackLayersMetadata) {
		BuildpackResolverContext resolverContext = new BuilderResolverContext(imageFetcher, builderMetadata,
				buildpackLayersMetadata);
		return BuildpackResolvers.resolveAll(resolverContext, request.getBuildpacks());
	}

	/**
	 * Executes the lifecycle of a build request using the provided ephemeral builder.
	 * @param request The build request to execute.
	 * @param builder The ephemeral builder to use for the build.
	 * @throws IOException If an I/O error occurs during the execution.
	 */
	private void executeLifecycle(BuildRequest request, EphemeralBuilder builder) throws IOException {
		ResolvedDockerHost dockerHost = null;
		if (this.dockerConfiguration != null && this.dockerConfiguration.isBindHostToBuilder()) {
			dockerHost = ResolvedDockerHost.from(this.dockerConfiguration.getHost());
		}
		try (Lifecycle lifecycle = new Lifecycle(this.log, this.docker, dockerHost, request, builder)) {
			lifecycle.execute();
		}
	}

	/**
	 * Tags an image with the given source reference and list of tags.
	 * @param sourceReference the reference to the source image
	 * @param tags the list of tags to be applied to the image
	 * @throws IOException if an I/O error occurs while tagging the image
	 */
	private void tagImage(ImageReference sourceReference, List<ImageReference> tags) throws IOException {
		for (ImageReference tag : tags) {
			this.docker.image().tag(sourceReference, tag);
			this.log.taggedImage(tag);
		}
	}

	/**
	 * Pushes the specified image and its associated tags to the repository.
	 * @param name the reference to the image to be pushed
	 * @param tags the list of tags associated with the image
	 * @throws IOException if an I/O error occurs while pushing the image
	 */
	private void pushImages(ImageReference name, List<ImageReference> tags) throws IOException {
		pushImage(name);
		for (ImageReference tag : tags) {
			pushImage(tag);
		}
	}

	/**
	 * Pushes an image to the Docker registry.
	 * @param reference the reference of the image to be pushed
	 * @throws IOException if an I/O error occurs during the push operation
	 */
	private void pushImage(ImageReference reference) throws IOException {
		Consumer<TotalProgressEvent> progressConsumer = this.log.pushingImage(reference);
		TotalProgressPushListener listener = new TotalProgressPushListener(progressConsumer);
		this.docker.image().push(reference, listener, getPublishAuthHeader());
		this.log.pushedImage(reference);
	}

	/**
	 * Returns the authentication header for the builder registry.
	 * @return the authentication header for the builder registry, or null if not
	 * available
	 */
	private String getBuilderAuthHeader() {
		return (this.dockerConfiguration != null && this.dockerConfiguration.getBuilderRegistryAuthentication() != null)
				? this.dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader() : null;
	}

	/**
	 * Returns the authentication header for publishing to the Docker registry.
	 * @return the authentication header, or null if not available
	 */
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

		/**
		 * Constructs a new ImageFetcher object with the specified domain, authentication
		 * header, and pull policy.
		 * @param domain the domain of the image source
		 * @param authHeader the authentication header to be used for accessing the image
		 * source
		 * @param pullPolicy the pull policy to determine how images should be fetched
		 */
		ImageFetcher(String domain, String authHeader, PullPolicy pullPolicy) {
			this.domain = domain;
			this.authHeader = authHeader;
			this.pullPolicy = pullPolicy;
		}

		/**
		 * Fetches an image of the specified type and reference.
		 * @param type the type of the image to fetch
		 * @param reference the reference to the image
		 * @return the fetched image
		 * @throws IOException if an I/O error occurs during the image fetching process
		 * @throws IllegalArgumentException if the type or reference is null
		 * @throws IllegalStateException if the image must be pulled from an authenticated
		 * registry but the authentication header is not set or the reference domain does
		 * not match the authenticated domain
		 * @throws DockerEngineException if an error occurs while inspecting the image
		 */
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

		/**
		 * Pulls an image from the specified reference and returns the pulled image.
		 * @param reference the reference of the image to be pulled
		 * @param imageType the type of the image to be pulled
		 * @return the pulled image
		 * @throws IOException if an I/O error occurs during the pull operation
		 */
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

		private final BuildpackLayersMetadata buildpackLayersMetadata;

		/**
		 * Constructs a new BuilderResolverContext object with the specified parameters.
		 * @param imageFetcher The ImageFetcher object used to fetch images.
		 * @param builderMetadata The BuilderMetadata object containing information about
		 * the builder.
		 * @param buildpackLayersMetadata The BuildpackLayersMetadata object containing
		 * information about the buildpack layers.
		 */
		BuilderResolverContext(ImageFetcher imageFetcher, BuilderMetadata builderMetadata,
				BuildpackLayersMetadata buildpackLayersMetadata) {
			this.imageFetcher = imageFetcher;
			this.builderMetadata = builderMetadata;
			this.buildpackLayersMetadata = buildpackLayersMetadata;
		}

		/**
		 * Retrieves the buildpack metadata from the builder metadata.
		 * @return the list of buildpack metadata
		 */
		@Override
		public List<BuildpackMetadata> getBuildpackMetadata() {
			return this.builderMetadata.getBuildpacks();
		}

		/**
		 * Returns the buildpack layers metadata.
		 * @return the buildpack layers metadata
		 */
		@Override
		public BuildpackLayersMetadata getBuildpackLayersMetadata() {
			return this.buildpackLayersMetadata;
		}

		/**
		 * Fetches an image based on the given image reference and image type.
		 * @param reference the reference to the image
		 * @param imageType the type of the image
		 * @return the fetched image
		 * @throws IOException if an I/O error occurs while fetching the image
		 */
		@Override
		public Image fetchImage(ImageReference reference, ImageType imageType) throws IOException {
			return this.imageFetcher.fetchImage(imageType, reference);
		}

		/**
		 * Exports the image layers for the given image reference.
		 * @param reference the reference to the image
		 * @param exports the consumer to handle the exported layer files
		 * @throws IOException if an I/O error occurs during the export process
		 */
		@Override
		public void exportImageLayers(ImageReference reference, IOBiConsumer<String, Path> exports) throws IOException {
			Builder.this.docker.image().exportLayerFiles(reference, exports);
		}

	}

}
