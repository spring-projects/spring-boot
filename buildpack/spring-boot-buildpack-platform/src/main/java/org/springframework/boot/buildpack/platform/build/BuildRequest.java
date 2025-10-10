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

import java.io.File;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.docker.ImagePlatform;
import org.springframework.boot.buildpack.platform.docker.type.Binding;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.io.Owner;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.util.Assert;

/**
 * A build request to be handled by the {@link Builder}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Andrey Shlykov
 * @author Jeroen Meijer
 * @author Rafael Ceccone
 * @author Julian Liebig
 * @since 2.3.0
 */
public class BuildRequest {

	static final String DEFAULT_BUILDER_IMAGE_NAME = "paketobuildpacks/builder-noble-java-tiny";

	static final String DEFAULT_BUILDER_IMAGE_REF = DEFAULT_BUILDER_IMAGE_NAME + ":latest";

	static final List<ImageReference> KNOWN_TRUSTED_BUILDERS = List.of(
			ImageReference.of("paketobuildpacks/builder-noble-java-tiny"),
			ImageReference.of("paketobuildpacks/builder-jammy-java-tiny"),
			ImageReference.of("paketobuildpacks/builder-jammy-tiny"),
			ImageReference.of("paketobuildpacks/builder-jammy-base"),
			ImageReference.of("paketobuildpacks/builder-jammy-full"),
			ImageReference.of("paketobuildpacks/builder-jammy-buildpackless-tiny"),
			ImageReference.of("paketobuildpacks/builder-jammy-buildpackless-base"),
			ImageReference.of("paketobuildpacks/builder-jammy-buildpackless-full"),
			ImageReference.of("gcr.io/buildpacks/builder"), ImageReference.of("heroku/builder"));

	private static final ImageReference DEFAULT_BUILDER = ImageReference.of(DEFAULT_BUILDER_IMAGE_REF);

	private final ImageReference name;

	private final Function<Owner, TarArchive> applicationContent;

	private final ImageReference builder;

	private final @Nullable Boolean trustBuilder;

	private final @Nullable ImageReference runImage;

	private final Creator creator;

	private final Map<String, String> env;

	private final boolean cleanCache;

	private final boolean verboseLogging;

	private final PullPolicy pullPolicy;

	private final boolean publish;

	private final List<BuildpackReference> buildpacks;

	private final List<Binding> bindings;

	private final @Nullable String network;

	private final List<ImageReference> tags;

	private final @Nullable Cache buildWorkspace;

	private final @Nullable Cache buildCache;

	private final @Nullable Cache launchCache;

	private final @Nullable Instant createdDate;

	private final @Nullable String applicationDirectory;

	private final @Nullable List<String> securityOptions;

	private final @Nullable ImagePlatform platform;

	BuildRequest(ImageReference name, Function<Owner, TarArchive> applicationContent) {
		Assert.notNull(name, "'name' must not be null");
		Assert.notNull(applicationContent, "'applicationContent' must not be null");
		this.name = name.inTaggedForm();
		this.applicationContent = applicationContent;
		this.builder = DEFAULT_BUILDER;
		this.trustBuilder = null;
		this.runImage = null;
		this.env = Collections.emptyMap();
		this.cleanCache = false;
		this.verboseLogging = false;
		this.pullPolicy = PullPolicy.ALWAYS;
		this.publish = false;
		this.creator = Creator.withVersion("");
		this.buildpacks = Collections.emptyList();
		this.bindings = Collections.emptyList();
		this.network = null;
		this.tags = Collections.emptyList();
		this.buildWorkspace = null;
		this.buildCache = null;
		this.launchCache = null;
		this.createdDate = null;
		this.applicationDirectory = null;
		this.securityOptions = null;
		this.platform = null;
	}

	BuildRequest(ImageReference name, Function<Owner, TarArchive> applicationContent, ImageReference builder,
			@Nullable Boolean trustBuilder, @Nullable ImageReference runImage, Creator creator, Map<String, String> env,
			boolean cleanCache, boolean verboseLogging, PullPolicy pullPolicy, boolean publish,
			List<BuildpackReference> buildpacks, List<Binding> bindings, @Nullable String network,
			List<ImageReference> tags, @Nullable Cache buildWorkspace, @Nullable Cache buildCache,
			@Nullable Cache launchCache, @Nullable Instant createdDate, @Nullable String applicationDirectory,
			@Nullable List<String> securityOptions, @Nullable ImagePlatform platform) {
		this.name = name;
		this.applicationContent = applicationContent;
		this.builder = builder;
		this.trustBuilder = trustBuilder;
		this.runImage = runImage;
		this.creator = creator;
		this.env = env;
		this.cleanCache = cleanCache;
		this.verboseLogging = verboseLogging;
		this.pullPolicy = pullPolicy;
		this.publish = publish;
		this.buildpacks = buildpacks;
		this.bindings = bindings;
		this.network = network;
		this.tags = tags;
		this.buildWorkspace = buildWorkspace;
		this.buildCache = buildCache;
		this.launchCache = launchCache;
		this.createdDate = createdDate;
		this.applicationDirectory = applicationDirectory;
		this.securityOptions = securityOptions;
		this.platform = platform;
	}

	/**
	 * Return a new {@link BuildRequest} with an updated builder.
	 * @param builder the new builder to use
	 * @return an updated build request
	 */
	public BuildRequest withBuilder(ImageReference builder) {
		Assert.notNull(builder, "'builder' must not be null");
		return new BuildRequest(this.name, this.applicationContent, builder.inTaggedOrDigestForm(), this.trustBuilder,
				this.runImage, this.creator, this.env, this.cleanCache, this.verboseLogging, this.pullPolicy,
				this.publish, this.buildpacks, this.bindings, this.network, this.tags, this.buildWorkspace,
				this.buildCache, this.launchCache, this.createdDate, this.applicationDirectory, this.securityOptions,
				this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with an updated trust builder setting.
	 * @param trustBuilder {@code true} if the builder should be treated as trusted,
	 * {@code false} otherwise
	 * @return an updated build request
	 * @since 3.4.0
	 */
	public BuildRequest withTrustBuilder(boolean trustBuilder) {
		return new BuildRequest(this.name, this.applicationContent, this.builder, trustBuilder, this.runImage,
				this.creator, this.env, this.cleanCache, this.verboseLogging, this.pullPolicy, this.publish,
				this.buildpacks, this.bindings, this.network, this.tags, this.buildWorkspace, this.buildCache,
				this.launchCache, this.createdDate, this.applicationDirectory, this.securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with an updated run image.
	 * @param runImageName the run image to use
	 * @return an updated build request
	 */
	public BuildRequest withRunImage(ImageReference runImageName) {
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder,
				runImageName.inTaggedOrDigestForm(), this.creator, this.env, this.cleanCache, this.verboseLogging,
				this.pullPolicy, this.publish, this.buildpacks, this.bindings, this.network, this.tags,
				this.buildWorkspace, this.buildCache, this.launchCache, this.createdDate, this.applicationDirectory,
				this.securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with an updated creator.
	 * @param creator the new {@code Creator} to use
	 * @return an updated build request
	 */
	public BuildRequest withCreator(Creator creator) {
		Assert.notNull(creator, "'creator' must not be null");
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				creator, this.env, this.cleanCache, this.verboseLogging, this.pullPolicy, this.publish, this.buildpacks,
				this.bindings, this.network, this.tags, this.buildWorkspace, this.buildCache, this.launchCache,
				this.createdDate, this.applicationDirectory, this.securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with an additional env variable.
	 * @param name the variable name
	 * @param value the variable value
	 * @return an updated build request
	 */
	public BuildRequest withEnv(String name, String value) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.hasText(value, "'value' must not be empty");
		Map<String, String> env = new LinkedHashMap<>(this.env);
		env.put(name, value);
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, Collections.unmodifiableMap(env), this.cleanCache, this.verboseLogging, this.pullPolicy,
				this.publish, this.buildpacks, this.bindings, this.network, this.tags, this.buildWorkspace,
				this.buildCache, this.launchCache, this.createdDate, this.applicationDirectory, this.securityOptions,
				this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with additional env variables.
	 * @param env the additional variables
	 * @return an updated build request
	 */
	public BuildRequest withEnv(Map<String, String> env) {
		Assert.notNull(env, "'env' must not be null");
		Map<String, String> updatedEnv = new LinkedHashMap<>(this.env);
		updatedEnv.putAll(env);
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, Collections.unmodifiableMap(updatedEnv), this.cleanCache, this.verboseLogging,
				this.pullPolicy, this.publish, this.buildpacks, this.bindings, this.network, this.tags,
				this.buildWorkspace, this.buildCache, this.launchCache, this.createdDate, this.applicationDirectory,
				this.securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with an updated clean cache setting.
	 * @param cleanCache if the cache should be cleaned
	 * @return an updated build request
	 */
	public BuildRequest withCleanCache(boolean cleanCache) {
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, this.env, cleanCache, this.verboseLogging, this.pullPolicy, this.publish, this.buildpacks,
				this.bindings, this.network, this.tags, this.buildWorkspace, this.buildCache, this.launchCache,
				this.createdDate, this.applicationDirectory, this.securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with an updated verbose logging setting.
	 * @param verboseLogging if verbose logging should be used
	 * @return an updated build request
	 */
	public BuildRequest withVerboseLogging(boolean verboseLogging) {
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, this.env, this.cleanCache, verboseLogging, this.pullPolicy, this.publish, this.buildpacks,
				this.bindings, this.network, this.tags, this.buildWorkspace, this.buildCache, this.launchCache,
				this.createdDate, this.applicationDirectory, this.securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with the updated image pull policy.
	 * @param pullPolicy image pull policy {@link PullPolicy}
	 * @return an updated build request
	 */
	public BuildRequest withPullPolicy(PullPolicy pullPolicy) {
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, this.env, this.cleanCache, this.verboseLogging, pullPolicy, this.publish, this.buildpacks,
				this.bindings, this.network, this.tags, this.buildWorkspace, this.buildCache, this.launchCache,
				this.createdDate, this.applicationDirectory, this.securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with an updated publish setting.
	 * @param publish if the built image should be pushed to a registry
	 * @return an updated build request
	 */
	public BuildRequest withPublish(boolean publish) {
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, this.env, this.cleanCache, this.verboseLogging, this.pullPolicy, publish, this.buildpacks,
				this.bindings, this.network, this.tags, this.buildWorkspace, this.buildCache, this.launchCache,
				this.createdDate, this.applicationDirectory, this.securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with an updated buildpacks setting.
	 * @param buildpacks a collection of buildpacks to use when building the image
	 * @return an updated build request
	 * @since 2.5.0
	 */
	public BuildRequest withBuildpacks(BuildpackReference... buildpacks) {
		Assert.notEmpty(buildpacks, "'buildpacks' must not be empty");
		return withBuildpacks(Arrays.asList(buildpacks));
	}

	/**
	 * Return a new {@link BuildRequest} with an updated buildpacks setting.
	 * @param buildpacks a collection of buildpacks to use when building the image
	 * @return an updated build request
	 * @since 2.5.0
	 */
	public BuildRequest withBuildpacks(List<BuildpackReference> buildpacks) {
		Assert.notNull(buildpacks, "'buildpacks' must not be null");
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, this.env, this.cleanCache, this.verboseLogging, this.pullPolicy, this.publish, buildpacks,
				this.bindings, this.network, this.tags, this.buildWorkspace, this.buildCache, this.launchCache,
				this.createdDate, this.applicationDirectory, this.securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with updated bindings.
	 * @param bindings a collection of bindings to mount to the build container
	 * @return an updated build request
	 * @since 2.5.0
	 */
	public BuildRequest withBindings(Binding... bindings) {
		Assert.notEmpty(bindings, "'bindings' must not be empty");
		return withBindings(Arrays.asList(bindings));
	}

	/**
	 * Return a new {@link BuildRequest} with updated bindings.
	 * @param bindings a collection of bindings to mount to the build container
	 * @return an updated build request
	 * @since 2.5.0
	 */
	public BuildRequest withBindings(List<Binding> bindings) {
		Assert.notNull(bindings, "'bindings' must not be null");
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, this.env, this.cleanCache, this.verboseLogging, this.pullPolicy, this.publish,
				this.buildpacks, bindings, this.network, this.tags, this.buildWorkspace, this.buildCache,
				this.launchCache, this.createdDate, this.applicationDirectory, this.securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with an updated network setting.
	 * @param network the network the build container will connect to
	 * @return an updated build request
	 * @since 2.6.0
	 */
	public BuildRequest withNetwork(@Nullable String network) {
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, this.env, this.cleanCache, this.verboseLogging, this.pullPolicy, this.publish,
				this.buildpacks, this.bindings, network, this.tags, this.buildWorkspace, this.buildCache,
				this.launchCache, this.createdDate, this.applicationDirectory, this.securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with updated tags.
	 * @param tags a collection of tags to be created for the built image
	 * @return an updated build request
	 */
	public BuildRequest withTags(ImageReference... tags) {
		Assert.notEmpty(tags, "'tags' must not be empty");
		return withTags(Arrays.asList(tags));
	}

	/**
	 * Return a new {@link BuildRequest} with updated tags.
	 * @param tags a collection of tags to be created for the built image
	 * @return an updated build request
	 */
	public BuildRequest withTags(List<ImageReference> tags) {
		Assert.notNull(tags, "'tags' must not be null");
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, this.env, this.cleanCache, this.verboseLogging, this.pullPolicy, this.publish,
				this.buildpacks, this.bindings, this.network, tags, this.buildWorkspace, this.buildCache,
				this.launchCache, this.createdDate, this.applicationDirectory, this.securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with an updated build workspace.
	 * @param buildWorkspace the build workspace
	 * @return an updated build request
	 * @since 3.2.0
	 */
	public BuildRequest withBuildWorkspace(Cache buildWorkspace) {
		Assert.notNull(buildWorkspace, "'buildWorkspace' must not be null");
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, this.env, this.cleanCache, this.verboseLogging, this.pullPolicy, this.publish,
				this.buildpacks, this.bindings, this.network, this.tags, buildWorkspace, this.buildCache,
				this.launchCache, this.createdDate, this.applicationDirectory, this.securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with an updated build cache.
	 * @param buildCache the build cache
	 * @return an updated build request
	 */
	public BuildRequest withBuildCache(Cache buildCache) {
		Assert.notNull(buildCache, "'buildCache' must not be null");
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, this.env, this.cleanCache, this.verboseLogging, this.pullPolicy, this.publish,
				this.buildpacks, this.bindings, this.network, this.tags, this.buildWorkspace, buildCache,
				this.launchCache, this.createdDate, this.applicationDirectory, this.securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with an updated launch cache.
	 * @param launchCache the cache
	 * @return an updated build request
	 */
	public BuildRequest withLaunchCache(Cache launchCache) {
		Assert.notNull(launchCache, "'launchCache' must not be null");
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, this.env, this.cleanCache, this.verboseLogging, this.pullPolicy, this.publish,
				this.buildpacks, this.bindings, this.network, this.tags, this.buildWorkspace, this.buildCache,
				launchCache, this.createdDate, this.applicationDirectory, this.securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with an updated created date.
	 * @param createdDate the created date
	 * @return an updated build request
	 */
	public BuildRequest withCreatedDate(String createdDate) {
		Assert.notNull(createdDate, "'createdDate' must not be null");
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, this.env, this.cleanCache, this.verboseLogging, this.pullPolicy, this.publish,
				this.buildpacks, this.bindings, this.network, this.tags, this.buildWorkspace, this.buildCache,
				this.launchCache, parseCreatedDate(createdDate), this.applicationDirectory, this.securityOptions,
				this.platform);
	}

	private Instant parseCreatedDate(String createdDate) {
		if ("now".equalsIgnoreCase(createdDate)) {
			return Instant.now();
		}
		try {
			return Instant.parse(createdDate);
		}
		catch (DateTimeParseException ex) {
			throw new IllegalArgumentException("Error parsing '" + createdDate + "' as an image created date", ex);
		}
	}

	/**
	 * Return a new {@link BuildRequest} with an updated application directory.
	 * @param applicationDirectory the application directory
	 * @return an updated build request
	 */
	public BuildRequest withApplicationDirectory(String applicationDirectory) {
		Assert.notNull(applicationDirectory, "'applicationDirectory' must not be null");
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, this.env, this.cleanCache, this.verboseLogging, this.pullPolicy, this.publish,
				this.buildpacks, this.bindings, this.network, this.tags, this.buildWorkspace, this.buildCache,
				this.launchCache, this.createdDate, applicationDirectory, this.securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with an updated security options.
	 * @param securityOptions the security options
	 * @return an updated build request
	 * @since 3.2.0
	 */
	public BuildRequest withSecurityOptions(List<String> securityOptions) {
		Assert.notNull(securityOptions, "'securityOptions' must not be null");
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, this.env, this.cleanCache, this.verboseLogging, this.pullPolicy, this.publish,
				this.buildpacks, this.bindings, this.network, this.tags, this.buildWorkspace, this.buildCache,
				this.launchCache, this.createdDate, this.applicationDirectory, securityOptions, this.platform);
	}

	/**
	 * Return a new {@link BuildRequest} with an updated image platform.
	 * @param platform the image platform
	 * @return an updated build request
	 * @since 3.4.0
	 */
	public BuildRequest withImagePlatform(String platform) {
		Assert.notNull(platform, "'platform' must not be null");
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.trustBuilder, this.runImage,
				this.creator, this.env, this.cleanCache, this.verboseLogging, this.pullPolicy, this.publish,
				this.buildpacks, this.bindings, this.network, this.tags, this.buildWorkspace, this.buildCache,
				this.launchCache, this.createdDate, this.applicationDirectory, this.securityOptions,
				ImagePlatform.of(platform));
	}

	/**
	 * Return the name of the image that should be created.
	 * @return the name of the image
	 */
	public ImageReference getName() {
		return this.name;
	}

	/**
	 * Return a {@link TarArchive} containing the application content that the buildpack
	 * should package. This is typically the contents of the Jar.
	 * @param owner the owner of the tar entries
	 * @return the application content
	 * @see TarArchive#fromZip(File, Owner)
	 */
	public TarArchive getApplicationContent(Owner owner) {
		return this.applicationContent.apply(owner);
	}

	/**
	 * Return the builder that should be used.
	 * @return the builder to use
	 */
	public ImageReference getBuilder() {
		return this.builder;
	}

	/**
	 * Return whether the builder should be treated as trusted.
	 * @return the trust builder flag
	 * @since 3.4.0
	 */
	public boolean isTrustBuilder() {
		return (this.trustBuilder != null) ? this.trustBuilder : isBuilderKnownAndTrusted();
	}

	private boolean isBuilderKnownAndTrusted() {
		return KNOWN_TRUSTED_BUILDERS.stream().anyMatch((builder) -> builder.getName().equals(this.builder.getName()));
	}

	/**
	 * Return the run image that should be used, if provided.
	 * @return the run image
	 */
	public @Nullable ImageReference getRunImage() {
		return this.runImage;
	}

	/**
	 * Return the {@link Creator} the builder should use.
	 * @return the {@code Creator}
	 */
	public Creator getCreator() {
		return this.creator;
	}

	/**
	 * Return any env variable that should be passed to the builder.
	 * @return the builder env
	 */
	public Map<String, String> getEnv() {
		return this.env;
	}

	/**
	 * Return if caches should be cleaned before packaging.
	 * @return if caches should be cleaned
	 */
	public boolean isCleanCache() {
		return this.cleanCache;
	}

	/**
	 * Return if verbose logging output should be used.
	 * @return if verbose logging should be used
	 */
	public boolean isVerboseLogging() {
		return this.verboseLogging;
	}

	/**
	 * Return if the built image should be pushed to a registry.
	 * @return if the built image should be pushed to a registry
	 */
	public boolean isPublish() {
		return this.publish;
	}

	/**
	 * Return the image {@link PullPolicy} that the builder should use.
	 * @return image pull policy
	 */
	public PullPolicy getPullPolicy() {
		return this.pullPolicy;
	}

	/**
	 * Return the collection of buildpacks to use when building the image, if provided.
	 * @return the buildpacks
	 */
	public List<BuildpackReference> getBuildpacks() {
		return this.buildpacks;
	}

	/**
	 * Return the collection of bindings to mount to the build container.
	 * @return the bindings
	 * @since 2.5.0
	 */
	public List<Binding> getBindings() {
		return this.bindings;
	}

	/**
	 * Return the network the build container will connect to.
	 * @return the network
	 * @since 2.6.0
	 */
	public @Nullable String getNetwork() {
		return this.network;
	}

	/**
	 * Return the collection of tags that should be created.
	 * @return the tags
	 */
	public List<ImageReference> getTags() {
		return this.tags;
	}

	/**
	 * Return the build workspace that should be used by the lifecycle.
	 * @return the build workspace or {@code null}
	 * @since 3.2.0
	 */
	public @Nullable Cache getBuildWorkspace() {
		return this.buildWorkspace;
	}

	/**
	 * Return the custom build cache that should be used by the lifecycle.
	 * @return the build cache
	 */
	public @Nullable Cache getBuildCache() {
		return this.buildCache;
	}

	/**
	 * Return the custom launch cache that should be used by the lifecycle.
	 * @return the launch cache
	 */
	public @Nullable Cache getLaunchCache() {
		return this.launchCache;
	}

	/**
	 * Return the custom created date that should be used by the lifecycle.
	 * @return the created date
	 */
	public @Nullable Instant getCreatedDate() {
		return this.createdDate;
	}

	/**
	 * Return the application directory that should be used by the lifecycle.
	 * @return the application directory
	 */
	public @Nullable String getApplicationDirectory() {
		return this.applicationDirectory;
	}

	/**
	 * Return the security options that should be used by the lifecycle.
	 * @return the security options or {@code null}
	 * @since 3.2.0
	 */
	public @Nullable List<String> getSecurityOptions() {
		return this.securityOptions;
	}

	/**
	 * Return the platform that should be used when pulling images.
	 * @return the platform or {@code null}
	 * @since 3.4.0
	 */
	public @Nullable ImagePlatform getImagePlatform() {
		return this.platform;
	}

	/**
	 * Factory method to create a new {@link BuildRequest} from a JAR file.
	 * @param jarFile the source jar file
	 * @return a new build request instance
	 */
	public static BuildRequest forJarFile(File jarFile) {
		assertJarFile(jarFile);
		return forJarFile(ImageReference.forJarFile(jarFile).inTaggedForm(), jarFile);
	}

	/**
	 * Factory method to create a new {@link BuildRequest} from a JAR file.
	 * @param name the name of the image that should be created
	 * @param jarFile the source jar file
	 * @return a new build request instance
	 */
	public static BuildRequest forJarFile(ImageReference name, File jarFile) {
		assertJarFile(jarFile);
		return new BuildRequest(name, (owner) -> TarArchive.fromZip(jarFile, owner));
	}

	/**
	 * Factory method to create a new {@link BuildRequest} with specific content.
	 * @param name the name of the image that should be created
	 * @param applicationContent function to provide the application content
	 * @return a new build request instance
	 */
	public static BuildRequest of(ImageReference name, Function<Owner, TarArchive> applicationContent) {
		return new BuildRequest(name, applicationContent);
	}

	private static void assertJarFile(File jarFile) {
		Assert.notNull(jarFile, "'jarFile' must not be null");
		Assert.isTrue(jarFile.exists(), "'jarFile' must exist");
		Assert.isTrue(jarFile.isFile(), "'jarFile' must be a file");
	}

}
