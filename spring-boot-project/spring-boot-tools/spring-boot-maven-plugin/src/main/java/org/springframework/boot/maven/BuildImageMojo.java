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
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.springframework.boot.buildpack.platform.build.AbstractBuildLog;
import org.springframework.boot.buildpack.platform.build.BuildLog;
import org.springframework.boot.buildpack.platform.build.BuildRequest;
import org.springframework.boot.buildpack.platform.build.Builder;
import org.springframework.boot.buildpack.platform.build.Creator;
import org.springframework.boot.buildpack.platform.docker.TotalProgressEvent;
import org.springframework.boot.buildpack.platform.io.Owner;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.boot.loader.tools.EntryWriter;
import org.springframework.boot.loader.tools.ImagePackager;
import org.springframework.boot.loader.tools.LayoutFactory;
import org.springframework.boot.loader.tools.Libraries;
import org.springframework.util.StringUtils;

/**
 * Package an application into a OCI image using a buildpack.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 */
@Mojo(name = "build-image", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
		requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class BuildImageMojo extends AbstractPackagerMojo {

	private static final String BUILDPACK_JVM_VERSION_KEY = "BP_JVM_VERSION";

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
	 * {@code env}, {@code cleanCache} and {@code verboseLogging} options.
	 * @since 2.3.0
	 */
	@Parameter
	private Image image;

	/**
	 * Alias for {@link Image#name} to support configuration via command-line property.
	 * @since 2.3.0
	 */
	@Parameter(property = "spring-boot.build-image.imageName", readonly = true)
	String imageName;

	/**
	 * Alias for {@link Image#builder} to support configuration via command-line property.
	 * @since 2.3.0
	 */
	@Parameter(property = "spring-boot.build-image.builder", readonly = true)
	String imageBuilder;

	/**
	 * Alias for {@link Image#runImage} to support configuration via command-line
	 * property.
	 * @since 2.3.1
	 */
	@Parameter(property = "spring-boot.build-image.runImage", readonly = true)
	String runImage;

	/**
	 * The type of archive (which corresponds to how the dependencies are laid out inside
	 * it). Possible values are {@code JAR}, {@code WAR}, {@code ZIP}, {@code DIR},
	 * {@code NONE}. Defaults to a guess based on the archive type.
	 * @since 2.3.11
	 */
	@Parameter
	private LayoutType layout;

	/**
	 * The layout factory that will be used to create the executable archive if no
	 * explicit layout is set. Alternative layouts implementations can be provided by 3rd
	 * parties.
	 * @since 2.3.11
	 */
	@Parameter
	private LayoutFactory layoutFactory;

	/**
	 * Return the type of archive that should be used when buiding the image.
	 * @return the value of the {@code layout} parameter, or {@code null} if the parameter
	 * is not provided
	 */
	@Override
	protected LayoutType getLayout() {
		return this.layout;
	}

	/**
	 * Return the layout factory that will be used to determine the {@link LayoutType} if
	 * no explicit layout is set.
	 * @return the value of the {@code layoutFactory} parameter, or {@code null} if the
	 * parameter is not provided
	 */
	@Override
	protected LayoutFactory getLayoutFactory() {
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
			Builder builder = new Builder(new MojoBuildLog(this::getLog));
			BuildRequest request = getBuildRequest(libraries);
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
		if (image.runImage == null && this.runImage != null) {
			image.setRunImage(this.runImage);
		}
		return customize(image.getBuildRequest(this.project.getArtifact(), content));
	}

	private TarArchive getApplicationContent(Owner owner, Libraries libraries, ImagePackager imagePackager) {
		ImagePackager packager = getConfiguredPackager(() -> imagePackager);
		return new PackagedTarArchive(owner, libraries, packager);
	}

	private File getArchiveFile() {
		// We can use 'project.getArtifact().getFile()' because that was done in a
		// forked lifecycle and is now null
		File archiveFile = getTargetFile(this.finalName, this.classifier, this.sourceDirectory);
		if (!archiveFile.exists()) {
			archiveFile = getSourceArtifact(this.classifier).getFile();
		}
		if (!archiveFile.exists()) {
			throw new IllegalStateException("Executable jar file required for building image");
		}
		if (archiveFile.getName().endsWith(".war")) {
			throw new IllegalStateException("Executable jar file required for building image");
		}
		return archiveFile;
	}

	/**
	 * Return the {@link File} to use to backup the original source.
	 * @return the file to use to backup the original source
	 */
	private File getBackupFile() {
		Artifact source = getSourceArtifact(null);
		if (this.classifier != null && !this.classifier.equals(source.getClassifier())) {
			return source.getFile();
		}
		return null;
	}

	private BuildRequest customize(BuildRequest request) {
		request = customizeEnvironment(request);
		request = customizeCreator(request);
		return request;
	}

	private BuildRequest customizeEnvironment(BuildRequest request) {
		if (!request.getEnv().containsKey(BUILDPACK_JVM_VERSION_KEY)) {
			JavaCompilerPluginConfiguration compilerConfiguration = new JavaCompilerPluginConfiguration(this.project);
			String targetJavaVersion = compilerConfiguration.getTargetMajorVersion();
			if (StringUtils.hasText(targetJavaVersion)) {
				return request.withEnv(BUILDPACK_JVM_VERSION_KEY, targetJavaVersion + ".*");
			}
		}
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
			this.packager.packageImage(this.libraries, (entry, entryWriter) -> write(entry, entryWriter, tar));
		}

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
