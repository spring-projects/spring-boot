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

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.DockerLog;
import org.springframework.boot.buildpack.platform.docker.ImagePlatform;
import org.springframework.boot.buildpack.platform.docker.TotalProgressEvent;
import org.springframework.boot.buildpack.platform.docker.TotalProgressPullListener;
import org.springframework.boot.buildpack.platform.docker.TotalProgressPushListener;
import org.springframework.boot.buildpack.platform.docker.UpdateListener;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerRegistryAuthentication;
import org.springframework.boot.buildpack.platform.docker.configuration.ResolvedDockerHost;
import org.springframework.boot.buildpack.platform.docker.transport.DockerEngineException;
import org.springframework.boot.buildpack.platform.docker.type.Binding;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageArchive;
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

	private final BuilderDockerConfiguration dockerConfiguration;

	/**
	 * Create a new builder instance.
	 */
	public Builder() {
		this(BuildLog.toSystemOut());
	}

	/**
	 * Create a new builder instance.
	 * @param dockerConfiguration the docker configuration
	 * @since 3.5.0
	 */
	public Builder(BuilderDockerConfiguration dockerConfiguration) {
		this(BuildLog.toSystemOut(), dockerConfiguration);
	}

	/**
	 * Create a new builder instance.
	 * @param log a logger used to record output
	 */
	public Builder(BuildLog log) {
		this(log, new DockerApi(null, BuildLogAdapter.get(log)), null);
	}

	/**
	 * Create a new builder instance.
	 * @param log a logger used to record output
	 * @param dockerConfiguration the docker configuration
	 * @since 3.5.0
	 */
	public Builder(BuildLog log, @Nullable BuilderDockerConfiguration dockerConfiguration) {
		this(log, new DockerApi((dockerConfiguration != null) ? dockerConfiguration.connection() : null,
				BuildLogAdapter.get(log)), dockerConfiguration);
	}

	Builder(BuildLog log, DockerApi docker, @Nullable BuilderDockerConfiguration dockerConfiguration) {
		Assert.notNull(log, "'log' must not be null");
		this.log = log;
		this.docker = docker;
		this.dockerConfiguration = (dockerConfiguration != null) ? dockerConfiguration
				: new BuilderDockerConfiguration();
	}

	public void build(BuildRequest request) throws DockerEngineException, IOException {
		Assert.notNull(request, "'request' must not be null");
		this.log.start(request);
		validateBindings(request.getBindings());
		PullPolicy pullPolicy = request.getPullPolicy();
		ImagePlatform platform = request.getImagePlatform();
		boolean specifiedPlatform = request.getImagePlatform() != null;
		DockerRegistryAuthentication registryAuthentication = this.dockerConfiguration.builderRegistryAuthentication();
		ImageFetcher imageFetcher = new ImageFetcher(registryAuthentication, pullPolicy);
		Image builderImage = imageFetcher.fetchImage(ImageType.BUILDER, request.getBuilder(), platform);
		BuilderMetadata builderMetadata = BuilderMetadata.fromImage(builderImage);
		request = withRunImageIfNeeded(request, builderMetadata);
		Assert.state(request.getRunImage() != null, "'request.getRunImage()' must not be null");
		platform = (platform != null) ? platform : ImagePlatform.from(builderImage);
		Image runImage = imageFetcher.fetchImage(ImageType.RUNNER, request.getRunImage(), platform);
		if (specifiedPlatform && runImage.getPrimaryDigest() != null) {
			request = request.withRunImage(request.getRunImage().withDigest(runImage.getPrimaryDigest()));
			runImage = imageFetcher.fetchImage(ImageType.RUNNER, request.getRunImage(), platform);
		}
		assertStackIdsMatch(runImage, builderImage);
		BuildOwner buildOwner = BuildOwner.fromEnv(builderImage.getConfig().getEnv());
		BuildpackLayersMetadata buildpackLayersMetadata = BuildpackLayersMetadata.fromImage(builderImage);
		Buildpacks buildpacks = getBuildpacks(request, imageFetcher, platform, builderMetadata,
				buildpackLayersMetadata);
		EphemeralBuilder ephemeralBuilder = new EphemeralBuilder(buildOwner, builderImage, request.getName(),
				builderMetadata, request.getCreator(), request.getEnv(), buildpacks);
		executeLifecycle(request, ephemeralBuilder);
		tagImage(request.getName(), request.getTags());
		if (request.isPublish()) {
			pushImages(request.getName(), request.getTags());
		}
	}

	private void validateBindings(List<Binding> bindings) {
		for (Binding binding : bindings) {
			if (binding.usesSensitiveContainerPath()) {
				this.log.sensitiveTargetBindingDetected(binding);
			}
		}
	}

	private BuildRequest withRunImageIfNeeded(BuildRequest request, BuilderMetadata metadata) {
		if (request.getRunImage() != null) {
			return request;
		}
		return request.withRunImage(getRunImageReference(metadata));
	}

	private ImageReference getRunImageReference(BuilderMetadata metadata) {
		if (metadata.getRunImages() != null && !metadata.getRunImages().isEmpty()) {
			String runImageName = metadata.getRunImages().get(0).getImage();
			return ImageReference.of(runImageName).inTaggedOrDigestForm();
		}
		String runImageName = metadata.getStack().getRunImage().getImage();
		Assert.state(StringUtils.hasText(runImageName), "Run image must be specified in the builder image metadata");
		return ImageReference.of(runImageName).inTaggedOrDigestForm();
	}

	private void assertStackIdsMatch(Image runImage, Image builderImage) {
		StackId runImageStackId = StackId.fromImage(runImage);
		StackId builderImageStackId = StackId.fromImage(builderImage);
		if (runImageStackId.hasId() && builderImageStackId.hasId()) {
			Assert.state(runImageStackId.equals(builderImageStackId), () -> "Run image stack '" + runImageStackId
					+ "' does not match builder stack '" + builderImageStackId + "'");
		}
	}

	private Buildpacks getBuildpacks(BuildRequest request, ImageFetcher imageFetcher, ImagePlatform platform,
			BuilderMetadata builderMetadata, BuildpackLayersMetadata buildpackLayersMetadata) {
		BuildpackResolverContext resolverContext = new BuilderResolverContext(imageFetcher, platform, builderMetadata,
				buildpackLayersMetadata);
		return BuildpackResolvers.resolveAll(resolverContext, request.getBuildpacks());
	}

	private void executeLifecycle(BuildRequest request, EphemeralBuilder builder) throws IOException {
		try (Lifecycle lifecycle = new Lifecycle(this.log, this.docker, getDockerHost(), request, builder)) {
			executeLifecycle(builder, lifecycle);
		}
	}

	private void executeLifecycle(EphemeralBuilder builder, Lifecycle lifecycle) throws IOException {
		ImageArchive archive = builder.getArchive(lifecycle.getApplicationDirectory());
		this.docker.image().load(archive, UpdateListener.none());
		try {
			lifecycle.execute();
		}
		finally {
			this.docker.image().remove(builder.getName(), true);
		}
	}

	private @Nullable ResolvedDockerHost getDockerHost() {
		boolean bindToBuilder = this.dockerConfiguration.bindHostToBuilder();
		return (bindToBuilder) ? ResolvedDockerHost.from(this.dockerConfiguration.connection()) : null;
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
		String authHeader = authHeader(this.dockerConfiguration.publishRegistryAuthentication(), reference);
		this.docker.image().push(reference, listener, authHeader);
		this.log.pushedImage(reference);
	}

	private static @Nullable String authHeader(@Nullable DockerRegistryAuthentication authentication,
			ImageReference reference) {
		return (authentication != null) ? authentication.getAuthHeader(reference) : null;
	}

	/**
	 * Internal utility class used to fetch images.
	 */
	private class ImageFetcher {

		private final @Nullable DockerRegistryAuthentication registryAuthentication;

		private final PullPolicy pullPolicy;

		ImageFetcher(@Nullable DockerRegistryAuthentication registryAuthentication, PullPolicy pullPolicy) {
			this.registryAuthentication = registryAuthentication;
			this.pullPolicy = pullPolicy;
		}

		Image fetchImage(ImageType type, ImageReference reference, @Nullable ImagePlatform platform)
				throws IOException {
			Assert.notNull(type, "'type' must not be null");
			Assert.notNull(reference, "'reference' must not be null");
			if (this.pullPolicy == PullPolicy.ALWAYS) {
				return pullImageAndCheckForPlatformMismatch(type, reference, platform);
			}
			try {
				Image image = Builder.this.docker.image().inspect(reference, platform);
				return checkPlatformMismatch(image, reference, platform);
			}
			catch (DockerEngineException ex) {
				if (this.pullPolicy == PullPolicy.IF_NOT_PRESENT && ex.getStatusCode() == 404) {
					return pullImageAndCheckForPlatformMismatch(type, reference, platform);
				}
				throw ex;
			}
		}

		private Image pullImageAndCheckForPlatformMismatch(ImageType type, ImageReference reference,
				@Nullable ImagePlatform platform) throws IOException {
			try {
				Image image = pullImage(reference, type, platform);
				return checkPlatformMismatch(image, reference, platform);
			}
			catch (DockerEngineException ex) {
				// Try to throw our own exception for consistent log output. Matching
				// on the message is a little brittle, but it doesn't matter too much
				// if it fails as the original exception is still enough to stop the build
				if (platform != null && ex.getMessage() != null
						&& ex.getMessage().contains("does not provide the specified platform")) {
					throwAsPlatformMismatchException(type, reference, platform, ex);
				}
				throw ex;
			}
		}

		private void throwAsPlatformMismatchException(ImageType type, ImageReference reference, ImagePlatform platform,
				@Nullable Throwable cause) throws IOException {
			try {
				Image image = pullImage(reference, type, null);
				throw new PlatformMismatchException(reference, platform, ImagePlatform.from(image), cause);
			}
			catch (DockerEngineException ex) {
			}
		}

		private Image pullImage(ImageReference reference, ImageType imageType, @Nullable ImagePlatform platform)
				throws IOException {
			TotalProgressPullListener listener = new TotalProgressPullListener(
					Builder.this.log.pullingImage(reference, platform, imageType));
			String authHeader = authHeader(this.registryAuthentication, reference);
			Image image = Builder.this.docker.image().pull(reference, platform, listener, authHeader);
			Builder.this.log.pulledImage(image, imageType);
			return image;
		}

		private Image checkPlatformMismatch(Image image, ImageReference reference,
				@Nullable ImagePlatform requestedPlatform) {
			if (requestedPlatform != null) {
				ImagePlatform actualPlatform = ImagePlatform.from(image);
				if (!actualPlatform.equals(requestedPlatform)) {
					throw new PlatformMismatchException(reference, requestedPlatform, actualPlatform, null);
				}
			}
			return image;
		}

	}

	private static final class PlatformMismatchException extends RuntimeException {

		private PlatformMismatchException(ImageReference imageReference, ImagePlatform requestedPlatform,
				ImagePlatform actualPlatform, @Nullable Throwable cause) {
			super("Image platform mismatch detected. The configured platform '%s' is not supported by the image '%s'. Requested platform '%s' but got '%s'"
				.formatted(requestedPlatform, imageReference, requestedPlatform, actualPlatform), cause);
		}

	}

	/**
	 * A {@link DockerLog} implementation that adapts to an {@link AbstractBuildLog}.
	 */
	static final class BuildLogAdapter implements DockerLog {

		private final AbstractBuildLog log;

		private BuildLogAdapter(AbstractBuildLog log) {
			this.log = log;
		}

		@Override
		public void log(String message) {
			this.log.log(message);
		}

		/**
		 * Creates {@link DockerLog} instance based on the provided {@link BuildLog}.
		 * <p>
		 * If the provided {@link BuildLog} instance is an {@link AbstractBuildLog}, the
		 * method returns a {@link BuildLogAdapter}, otherwise it returns a default
		 * {@link DockerLog#toSystemOut()}.
		 * @param log the {@link BuildLog} instance to delegate
		 * @return a {@link DockerLog} instance for logging
		 */
		static DockerLog get(BuildLog log) {
			if (log instanceof AbstractBuildLog abstractBuildLog) {
				return new BuildLogAdapter(abstractBuildLog);
			}
			return DockerLog.toSystemOut();
		}

	}

	/**
	 * {@link BuildpackResolverContext} implementation for the {@link Builder}.
	 */
	private class BuilderResolverContext implements BuildpackResolverContext {

		private final ImageFetcher imageFetcher;

		private final ImagePlatform platform;

		private final BuilderMetadata builderMetadata;

		private final BuildpackLayersMetadata buildpackLayersMetadata;

		BuilderResolverContext(ImageFetcher imageFetcher, ImagePlatform platform, BuilderMetadata builderMetadata,
				BuildpackLayersMetadata buildpackLayersMetadata) {
			this.imageFetcher = imageFetcher;
			this.platform = platform;
			this.builderMetadata = builderMetadata;
			this.buildpackLayersMetadata = buildpackLayersMetadata;
		}

		@Override
		public List<BuildpackMetadata> getBuildpackMetadata() {
			return this.builderMetadata.getBuildpacks();
		}

		@Override
		public BuildpackLayersMetadata getBuildpackLayersMetadata() {
			return this.buildpackLayersMetadata;
		}

		@Override
		public Image fetchImage(ImageReference reference, ImageType imageType) throws IOException {
			return this.imageFetcher.fetchImage(imageType, reference, this.platform);
		}

		@Override
		public void exportImageLayers(ImageReference reference, IOBiConsumer<String, TarArchive> exports)
				throws IOException {
			Builder.this.docker.image().exportLayers(reference, this.platform, exports);
		}

	}

}
