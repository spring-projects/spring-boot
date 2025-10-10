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

package org.springframework.boot.maven;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.build.AbstractBuildLog;
import org.springframework.boot.buildpack.platform.build.BuildLog;
import org.springframework.boot.buildpack.platform.build.BuildRequest;
import org.springframework.boot.buildpack.platform.build.Builder;
import org.springframework.boot.buildpack.platform.build.BuilderDockerConfiguration;
import org.springframework.boot.buildpack.platform.build.Creator;
import org.springframework.boot.buildpack.platform.build.PullPolicy;
import org.springframework.boot.buildpack.platform.docker.TotalProgressEvent;
import org.springframework.boot.buildpack.platform.io.Owner;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.boot.loader.tools.EntryWriter;
import org.springframework.boot.loader.tools.ImagePackager;
import org.springframework.boot.loader.tools.LayoutFactory;
import org.springframework.boot.loader.tools.Libraries;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Package an application into an OCI image using a buildpack.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jeroen Meijer
 * @since 2.3.0
 */
public abstract class BuildImageMojo extends AbstractPackagerMojo {

	static {
		System.setProperty("org.slf4j.simpleLogger.log.org.apache.http.wire", "ERROR");
	}

	/**
	 * Directory containing the source archive.
	 * @since 2.3.0
	 */
	@Parameter(defaultValue = "${project.build.directory}", required = true)
	@SuppressWarnings("NullAway.Init")
	private File sourceDirectory;

	/**
	 * Name of the source archive.
	 * @since 2.3.0
	 */
	@Parameter(defaultValue = "${project.build.finalName}", readonly = true, required = true)
	@SuppressWarnings("NullAway.Init")
	private String finalName;

	/**
	 * Skip the execution.
	 * @since 2.3.0
	 */
	@Parameter(property = "spring-boot.build-image.skip", defaultValue = "false")
	private boolean skip;

	/**
	 * Classifier used when finding the source archive.
	 * @since 2.3.0
	 */
	@Parameter
	private @Nullable String classifier;

	/**
	 * Image configuration, with {@code builder}, {@code runImage}, {@code name},
	 * {@code env}, {@code cleanCache}, {@code verboseLogging}, {@code pullPolicy}, and
	 * {@code publish} options.
	 * @since 2.3.0
	 */
	@Parameter
	private @Nullable Image image;

	/**
	 * Alias for {@link Image#name} to support configuration through command-line
	 * property.
	 * @since 2.3.0
	 */
	@Parameter(property = "spring-boot.build-image.imageName")
	@Nullable String imageName;

	/**
	 * Alias for {@link Image#builder} to support configuration through command-line
	 * property.
	 * @since 2.3.0
	 */
	@Parameter(property = "spring-boot.build-image.builder")
	@Nullable String imageBuilder;

	/**
	 * Alias for {@link Image#trustBuilder} to support configuration through command-line
	 * property.
	 */
	@Parameter(property = "spring-boot.build-image.trustBuilder")
	@Nullable Boolean trustBuilder;

	/**
	 * Alias for {@link Image#runImage} to support configuration through command-line
	 * property.
	 * @since 2.3.1
	 */
	@Parameter(property = "spring-boot.build-image.runImage")
	@Nullable String runImage;

	/**
	 * Alias for {@link Image#cleanCache} to support configuration through command-line
	 * property.
	 * @since 2.4.0
	 */
	@Parameter(property = "spring-boot.build-image.cleanCache")
	@Nullable Boolean cleanCache;

	/**
	 * Alias for {@link Image#pullPolicy} to support configuration through command-line
	 * property.
	 */
	@Parameter(property = "spring-boot.build-image.pullPolicy")
	@Nullable PullPolicy pullPolicy;

	/**
	 * Alias for {@link Image#publish} to support configuration through command-line
	 * property.
	 */
	@Parameter(property = "spring-boot.build-image.publish")
	@Nullable Boolean publish;

	/**
	 * Alias for {@link Image#network} to support configuration through command-line
	 * property.
	 * @since 2.6.0
	 */
	@Parameter(property = "spring-boot.build-image.network")
	@Nullable String network;

	/**
	 * Alias for {@link Image#createdDate} to support configuration through command-line
	 * property.
	 * @since 3.1.0
	 */
	@Parameter(property = "spring-boot.build-image.createdDate")
	@Nullable String createdDate;

	/**
	 * Alias for {@link Image#applicationDirectory} to support configuration through
	 * command-line property.
	 * @since 3.1.0
	 */
	@Parameter(property = "spring-boot.build-image.applicationDirectory")
	@Nullable String applicationDirectory;

	/**
	 * Alias for {@link Image#imagePlatform} to support configuration through command-line
	 * property.
	 * @since 3.4.0
	 */
	@Parameter(property = "spring-boot.build-image.imagePlatform")
	@Nullable String imagePlatform;

	/**
	 * Docker configuration options.
	 * @since 2.4.0
	 */
	@Parameter
	private @Nullable Docker docker;

	/**
	 * The type of archive (which corresponds to how the dependencies are laid out inside
	 * it). Possible values are {@code JAR}, {@code WAR}, {@code ZIP}, {@code DIR},
	 * {@code NONE}. Defaults to a guess based on the archive type.
	 * @since 2.3.11
	 */
	@Parameter
	private @Nullable LayoutType layout;

	/**
	 * The layout factory that will be used to create the executable archive if no
	 * explicit layout is set. Alternative layouts implementations can be provided by 3rd
	 * parties.
	 * @since 2.3.11
	 */
	@Parameter
	private @Nullable LayoutFactory layoutFactory;

	protected BuildImageMojo(MavenProjectHelper projectHelper) {
		super(projectHelper);
	}

	/**
	 * Return the type of archive that should be used when building the image.
	 * @return the value of the {@code layout} parameter, or {@code null} if the parameter
	 * is not provided
	 */
	@Override
	protected @Nullable LayoutType getLayout() {
		return this.layout;
	}

	/**
	 * Return the layout factory that will be used to determine the
	 * {@link AbstractPackagerMojo.LayoutType} if no explicit layout is set.
	 * @return the value of the {@code layoutFactory} parameter, or {@code null} if the
	 * parameter is not provided
	 */
	@Override
	protected @Nullable LayoutFactory getLayoutFactory() {
		return this.layoutFactory;
	}

	@Override
	public void execute() throws MojoExecutionException {
		if (this.project.getPackaging().equals("pom")) {
			getLog().debug("build-image goal could not be applied to pom project.");
			return;
		}
		if (this.skip) {
			getLog().debug("skipping build-image as per configuration.");
			return;
		}
		buildImage();
	}

	private void buildImage() throws MojoExecutionException {
		Libraries libraries = getLibraries(Collections.emptySet());
		try {
			BuildRequest request = getBuildRequest(libraries);
			Docker docker = (this.docker != null) ? this.docker : new Docker();
			BuilderDockerConfiguration dockerConfiguration = docker.asDockerConfiguration(getLog(),
					request.isPublish());
			Builder builder = new Builder(new MojoBuildLog(this::getLog), dockerConfiguration);
			builder.build(request);
		}
		catch (IOException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}

	private BuildRequest getBuildRequest(Libraries libraries) {
		ImagePackager imagePackager = new ImagePackager(getArchiveFile(), getBackupFile());
		Function<Owner, TarArchive> content = (owner) -> getApplicationContent(owner, libraries, imagePackager);
		Image image = (this.image != null) ? this.image : new Image();
		if (image.name == null && this.imageName != null) {
			image.setName(this.imageName);
		}
		if (image.builder == null && this.imageBuilder != null) {
			image.setBuilder(this.imageBuilder);
		}
		if (image.trustBuilder == null && this.trustBuilder != null) {
			image.setTrustBuilder(this.trustBuilder);
		}
		if (image.runImage == null && this.runImage != null) {
			image.setRunImage(this.runImage);
		}
		if (image.cleanCache == null && this.cleanCache != null) {
			image.setCleanCache(this.cleanCache);
		}
		if (image.pullPolicy == null && this.pullPolicy != null) {
			image.setPullPolicy(this.pullPolicy);
		}
		if (image.publish == null && this.publish != null) {
			image.setPublish(this.publish);
		}
		if (image.network == null && this.network != null) {
			image.setNetwork(this.network);
		}
		if (image.createdDate == null && this.createdDate != null) {
			image.setCreatedDate(this.createdDate);
		}
		if (image.applicationDirectory == null && this.applicationDirectory != null) {
			image.setApplicationDirectory(this.applicationDirectory);
		}
		if (image.imagePlatform == null && this.imagePlatform != null) {
			image.setImagePlatform(this.imagePlatform);
		}
		return customize(image.getBuildRequest(this.project.getArtifact(), content));
	}

	private TarArchive getApplicationContent(Owner owner, Libraries libraries, ImagePackager imagePackager) {
		ImagePackager packager = getConfiguredPackager(() -> imagePackager);
		return new PackagedTarArchive(owner, libraries, packager);
	}

	private File getArchiveFile() {
		// We can't use 'project.getArtifact().getFile()' because package can be done in a
		// forked lifecycle and will be null
		File archiveFile = getTargetFile(this.finalName, this.classifier, this.sourceDirectory);
		if (!archiveFile.exists()) {
			archiveFile = getSourceArtifact(this.classifier).getFile();
		}
		if (!archiveFile.exists()) {
			throw new IllegalStateException("A jar or war file is required for building image");
		}
		return archiveFile;
	}

	/**
	 * Return the {@link File} to use to back up the original source.
	 * @return the file to use to back up the original source
	 */
	private @Nullable File getBackupFile() {
		// We can't use 'project.getAttachedArtifacts()' because package can be done in a
		// forked lifecycle and will be null
		if (this.classifier != null) {
			File backupFile = getTargetFile(this.finalName, null, this.sourceDirectory);
			if (backupFile.exists()) {
				return backupFile;
			}
			Artifact source = getSourceArtifact(null);
			if (!this.classifier.equals(source.getClassifier())) {
				return source.getFile();
			}
		}
		return null;
	}

	private BuildRequest customize(BuildRequest request) {
		request = customizeCreator(request);
		return request;
	}

	private BuildRequest customizeCreator(BuildRequest request) {
		String springBootVersion = VersionExtractor.forClass(BuildImageMojo.class);
		if (StringUtils.hasText(springBootVersion)) {
			request = request.withCreator(Creator.withVersion(springBootVersion));
		}
		return request;
	}

	/**
	 * {@link BuildLog} backed by Mojo logging.
	 */
	private static class MojoBuildLog extends AbstractBuildLog {

		private static final long THRESHOLD = Duration.ofSeconds(2).toMillis();

		private final Supplier<Log> log;

		MojoBuildLog(Supplier<Log> log) {
			this.log = log;
		}

		@Override
		protected void log(String message) {
			this.log.get().info(message);
		}

		@Override
		protected Consumer<TotalProgressEvent> getProgressConsumer(String message) {
			return new ProgressLog(message);
		}

		private class ProgressLog implements Consumer<TotalProgressEvent> {

			private final String message;

			private long last;

			ProgressLog(String message) {
				this.message = message;
				this.last = System.currentTimeMillis();
			}

			@Override
			public void accept(TotalProgressEvent progress) {
				log(progress.getPercent());
			}

			private void log(int percent) {
				if (percent == 100 || (System.currentTimeMillis() - this.last) > THRESHOLD) {
					MojoBuildLog.this.log.get().info(this.message + " " + percent + "%");
					this.last = System.currentTimeMillis();
				}
			}

		}

	}

	/**
	 * Adapter class to expose the packaged jar as a {@link TarArchive}.
	 */
	static class PackagedTarArchive implements TarArchive {

		static final long NORMALIZED_MOD_TIME = TarArchive.NORMALIZED_TIME.toEpochMilli();

		private final Owner owner;

		private final Libraries libraries;

		private final ImagePackager packager;

		PackagedTarArchive(Owner owner, Libraries libraries, ImagePackager packager) {
			this.owner = owner;
			this.libraries = libraries;
			this.packager = packager;
		}

		@Override
		public void writeTo(OutputStream outputStream) throws IOException {
			TarArchiveOutputStream tar = new TarArchiveOutputStream(outputStream);
			tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			try {
				this.packager.packageImage(this.libraries, (entry, entryWriter) -> write(entry, entryWriter, tar));
			}
			catch (RuntimeException ex) {
				outputStream.close();
				throw new RuntimeException("Error packaging archive for image", ex);
			}
		}

		private void write(ZipEntry jarEntry, @Nullable EntryWriter entryWriter, TarArchiveOutputStream tar) {
			try {
				TarArchiveEntry tarEntry = convert(jarEntry);
				tar.putArchiveEntry(tarEntry);
				if (tarEntry.isFile()) {
					Assert.state(entryWriter != null, "'entryWriter' must not be null");
					entryWriter.write(tar);
				}
				tar.closeArchiveEntry();
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

		private TarArchiveEntry convert(ZipEntry entry) {
			byte linkFlag = (entry.isDirectory()) ? TarConstants.LF_DIR : TarConstants.LF_NORMAL;
			TarArchiveEntry tarEntry = new TarArchiveEntry(entry.getName(), linkFlag, true);
			tarEntry.setUserId(this.owner.getUid());
			tarEntry.setGroupId(this.owner.getGid());
			tarEntry.setModTime(NORMALIZED_MOD_TIME);
			if (!entry.isDirectory()) {
				tarEntry.setSize(entry.getSize());
			}
			return tarEntry;
		}

	}

}
