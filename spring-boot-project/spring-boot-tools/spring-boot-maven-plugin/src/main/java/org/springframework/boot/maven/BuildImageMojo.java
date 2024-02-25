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

import org.springframework.boot.buildpack.platform.build.AbstractBuildLog;
import org.springframework.boot.buildpack.platform.build.BuildLog;
import org.springframework.boot.buildpack.platform.build.BuildRequest;
import org.springframework.boot.buildpack.platform.build.Builder;
import org.springframework.boot.buildpack.platform.build.Creator;
import org.springframework.boot.buildpack.platform.build.PullPolicy;
import org.springframework.boot.buildpack.platform.docker.TotalProgressEvent;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration;
import org.springframework.boot.buildpack.platform.io.Owner;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.boot.loader.tools.EntryWriter;
import org.springframework.boot.loader.tools.ImagePackager;
import org.springframework.boot.loader.tools.LayoutFactory;
import org.springframework.boot.loader.tools.Libraries;
import org.springframework.boot.loader.tools.LoaderImplementation;
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
	private File sourceDirectory;

	/**
	 * Name of the source archive.
	 * @since 2.3.0
	 */
	@Parameter(defaultValue = "${project.build.finalName}", readonly = true)
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
	private String classifier;

	/**
	 * Image configuration, with {@code builder}, {@code runImage}, {@code name},
	 * {@code env}, {@code cleanCache}, {@code verboseLogging}, {@code pullPolicy}, and
	 * {@code publish} options.
	 * @since 2.3.0
	 */
	@Parameter
	private Image image;

	/**
	 * Alias for {@link Image#name} to support configuration through command-line
	 * property.
	 * @since 2.3.0
	 */
	@Parameter(property = "spring-boot.build-image.imageName", readonly = true)
	String imageName;

	/**
	 * Alias for {@link Image#builder} to support configuration through command-line
	 * property.
	 * @since 2.3.0
	 */
	@Parameter(property = "spring-boot.build-image.builder", readonly = true)
	String imageBuilder;

	/**
	 * Alias for {@link Image#runImage} to support configuration through command-line
	 * property.
	 * @since 2.3.1
	 */
	@Parameter(property = "spring-boot.build-image.runImage", readonly = true)
	String runImage;

	/**
	 * Alias for {@link Image#cleanCache} to support configuration through command-line
	 * property.
	 * @since 2.4.0
	 */
	@Parameter(property = "spring-boot.build-image.cleanCache", readonly = true)
	Boolean cleanCache;

	/**
	 * Alias for {@link Image#pullPolicy} to support configuration through command-line
	 * property.
	 */
	@Parameter(property = "spring-boot.build-image.pullPolicy", readonly = true)
	PullPolicy pullPolicy;

	/**
	 * Alias for {@link Image#publish} to support configuration through command-line
	 * property.
	 */
	@Parameter(property = "spring-boot.build-image.publish", readonly = true)
	Boolean publish;

	/**
	 * Alias for {@link Image#network} to support configuration through command-line
	 * property.
	 * @since 2.6.0
	 */
	@Parameter(property = "spring-boot.build-image.network", readonly = true)
	String network;

	/**
	 * Alias for {@link Image#createdDate} to support configuration through command-line
	 * property.
	 * @since 3.1.0
	 */
	@Parameter(property = "spring-boot.build-image.createdDate", readonly = true)
	String createdDate;

	/**
	 * Alias for {@link Image#applicationDirectory} to support configuration through
	 * command-line property.
	 * @since 3.1.0
	 */
	@Parameter(property = "spring-boot.build-image.applicationDirectory", readonly = true)
	String applicationDirectory;

	/**
	 * Docker configuration options.
	 * @since 2.4.0
	 */
	@Parameter
	private Docker docker;

	/**
	 * The type of archive (which corresponds to how the dependencies are laid out inside
	 * it). Possible values are {@code JAR}, {@code WAR}, {@code ZIP}, {@code DIR},
	 * {@code NONE}. Defaults to a guess based on the archive type.
	 * @since 2.3.11
	 */
	@Parameter
	private LayoutType layout;

	/**
	 * The loader implementation that should be used.
	 * @since 3.2.0
	 */
	@Parameter
	private LoaderImplementation loaderImplementation;

	/**
	 * The layout factory that will be used to create the executable archive if no
	 * explicit layout is set. Alternative layouts implementations can be provided by 3rd
	 * parties.
	 * @since 2.3.11
	 */
	@Parameter
	private LayoutFactory layoutFactory;

	/**
	 * Return the type of archive that should be used when building the image.
	 * @return the value of the {@code layout} parameter, or {@code null} if the parameter
	 * is not provided
	 */
	@Override
	protected LayoutType getLayout() {
		return this.layout;
	}

	/**
	 * Returns the loader implementation used by this BuildImageMojo.
	 * @return the loader implementation used by this BuildImageMojo
	 */
	@Override
	protected LoaderImplementation getLoaderImplementation() {
		return this.loaderImplementation;
	}

	/**
	 * Return the layout factory that will be used to determine the
	 * {@link AbstractPackagerMojo.LayoutType} if no explicit layout is set.
	 * @return the value of the {@code layoutFactory} parameter, or {@code null} if the
	 * parameter is not provided
	 */
	@Override
	protected LayoutFactory getLayoutFactory() {
		return this.layoutFactory;
	}

	/**
	 * Executes the build-image goal.
	 * @throws MojoExecutionException if an error occurs during execution
	 */
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

	/**
	 * Builds the image using the specified libraries and Docker configuration.
	 * @throws MojoExecutionException if an error occurs during the build process
	 */
	private void buildImage() throws MojoExecutionException {
		Libraries libraries = getLibraries(Collections.emptySet());
		try {
			DockerConfiguration dockerConfiguration = (this.docker != null) ? this.docker.asDockerConfiguration()
					: new Docker().asDockerConfiguration();
			BuildRequest request = getBuildRequest(libraries);
			Builder builder = new Builder(new MojoBuildLog(this::getLog), dockerConfiguration);
			builder.build(request);
		}
		catch (IOException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}

	/**
	 * Retrieves the build request for creating an image.
	 * @param libraries The libraries required for the image.
	 * @return The build request for creating the image.
	 */
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
		return customize(image.getBuildRequest(this.project.getArtifact(), content));
	}

	/**
	 * Retrieves the application content as a TarArchive.
	 * @param owner the owner of the application
	 * @param libraries the libraries required by the application
	 * @param imagePackager the image packager used to package the application
	 * @return the packaged TarArchive containing the application content
	 */
	private TarArchive getApplicationContent(Owner owner, Libraries libraries, ImagePackager imagePackager) {
		ImagePackager packager = getConfiguredPackager(() -> imagePackager);
		return new PackagedTarArchive(owner, libraries, packager);
	}

	/**
	 * Retrieves the archive file for building the image.
	 * @return The archive file.
	 * @throws IllegalStateException if a jar or war file is required for building the
	 * image and cannot be found.
	 */
	private File getArchiveFile() {
		// We can use 'project.getArtifact().getFile()' because that was done in a
		// forked lifecycle and is now null
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
	private File getBackupFile() {
		Artifact source = getSourceArtifact(null);
		if (this.classifier != null && !this.classifier.equals(source.getClassifier())) {
			return source.getFile();
		}
		return null;
	}

	/**
	 * Customizes the given BuildRequest by calling the customizeCreator method.
	 * @param request the BuildRequest to be customized
	 * @return the customized BuildRequest
	 */
	private BuildRequest customize(BuildRequest request) {
		request = customizeCreator(request);
		return request;
	}

	/**
	 * Customizes the creator of the build request based on the Spring Boot version.
	 * @param request the original build request
	 * @return the build request with the creator customized based on the Spring Boot
	 * version
	 */
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

		/**
		 * Constructs a new MojoBuildLog with the specified log supplier.
		 * @param log the supplier of the log to be used by the MojoBuildLog
		 */
		MojoBuildLog(Supplier<Log> log) {
			this.log = log;
		}

		/**
		 * Logs a message to the build log.
		 * @param message the message to be logged
		 */
		@Override
		protected void log(String message) {
			this.log.get().info(message);
		}

		/**
		 * Returns a Consumer object that logs the progress of a TotalProgressEvent.
		 * @param message the message to be logged along with the progress
		 * @return a Consumer object that logs the progress of a TotalProgressEvent
		 */
		@Override
		protected Consumer<TotalProgressEvent> getProgressConsumer(String message) {
			return new ProgressLog(message);
		}

		/**
		 * ProgressLog class.
		 */
		private class ProgressLog implements Consumer<TotalProgressEvent> {

			private final String message;

			private long last;

			/**
			 * Creates a new instance of ProgressLog with the given message and sets the
			 * current time as the last logged time.
			 * @param message the message to be logged
			 */
			ProgressLog(String message) {
				this.message = message;
				this.last = System.currentTimeMillis();
			}

			/**
			 * Accepts a TotalProgressEvent and logs the progress percentage.
			 * @param progress the TotalProgressEvent object containing the progress
			 * information
			 */
			@Override
			public void accept(TotalProgressEvent progress) {
				log(progress.getPercent());
			}

			/**
			 * Logs the progress percentage.
			 * @param percent the progress percentage
			 */
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

		/**
		 * Constructs a new PackagedTarArchive object with the specified owner, libraries,
		 * and packager.
		 * @param owner the owner of the tar archive
		 * @param libraries the libraries to be included in the tar archive
		 * @param packager the image packager to be used for packaging the tar archive
		 */
		PackagedTarArchive(Owner owner, Libraries libraries, ImagePackager packager) {
			this.owner = owner;
			this.libraries = libraries;
			this.packager = packager;
		}

		/**
		 * Writes the packaged tar archive to the specified output stream.
		 * @param outputStream the output stream to write the tar archive to
		 * @throws IOException if an I/O error occurs while writing the tar archive
		 * @throws RuntimeException if an error occurs while packaging the archive for the
		 * image
		 */
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

		/**
		 * Writes a ZipEntry to a TarArchiveOutputStream.
		 * @param jarEntry The ZipEntry to be written.
		 * @param entryWriter The EntryWriter used to write the contents of the ZipEntry
		 * to the TarArchiveOutputStream.
		 * @param tar The TarArchiveOutputStream to write the ZipEntry to.
		 * @throws IllegalStateException If an IOException occurs during the writing
		 * process.
		 */
		private void write(ZipEntry jarEntry, EntryWriter entryWriter, TarArchiveOutputStream tar) {
			try {
				TarArchiveEntry tarEntry = convert(jarEntry);
				tar.putArchiveEntry(tarEntry);
				if (tarEntry.isFile()) {
					entryWriter.write(tar);
				}
				tar.closeArchiveEntry();
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

		/**
		 * Converts a ZipEntry to a TarArchiveEntry.
		 * @param entry the ZipEntry to convert
		 * @return the converted TarArchiveEntry
		 */
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
