/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.bootstrap.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.zip.ZipEntry;
import org.codehaus.plexus.archiver.zip.ZipFile;
import org.codehaus.plexus.archiver.zip.ZipResource;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactDescriptorRequest;
import org.sonatype.aether.resolution.ArtifactDescriptorResult;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * Abstract base of MOJOs that package executable archives.
 * 
 * @author Phillip Webb
 */
public abstract class AbstractExecutableArchiveMojo extends AbstractMojo {

	private static final String[] DEFAULT_EXCLUDES = new String[] { "**/package.html" };

	private static final String[] DEFAULT_INCLUDES = new String[] { "**/**" };

	private static final String MAIN_CLASS_ATTRIBUTE = "Main-Class";

	private static final String START_CLASS_ATTRIBUTE = "Start-Class";

	/**
	 * Archiver used to create a JAR file.
	 */
	@Component(role = Archiver.class, hint = "jar")
	private JarArchiver jarArchiver;

	/**
	 * Maven project helper utils.
	 */
	@Component
	private MavenProjectHelper projectHelper;

	/**
	 * Aether repository system used to download artifacts.
	 */
	@Component
	private RepositorySystem repositorySystem;

	/**
	 * The Maven project.
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * The Maven session.
	 */
	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession session;

	/**
	 * Directory containing the generated archive.
	 */
	@Parameter(defaultValue = "${project.build.directory}", required = true)
	private File outputDirectory;

	/**
	 * Name of the generated archive.
	 */
	@Parameter(defaultValue = "${project.build.finalName}", required = true)
	private String finalName;

	/**
	 * The name of the main class. If not specified the first compiled class found that
	 * contains a 'main' method will be used.
	 */
	@Parameter
	private String mainClass;

	/**
	 * Classifier to add to the artifact generated. If given, the artifact will be
	 * attached. If this is not given,it will merely be written to the output directory
	 * according to the finalName.
	 */
	@Parameter
	private String classifier;

	/**
	 * List of files to include. Specified as fileset patterns which are relative to the
	 * input directory whose contents is being packaged into the archive.
	 */
	@Parameter
	private String[] includes;

	/**
	 * List of files to exclude. Specified as fileset patterns which are relative to the
	 * input directory whose contents is being packaged into the archive.
	 */
	@Parameter
	private String[] excludes;

	/**
	 * Directory containing the classes and resource files that should be packaged into
	 * the archive.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	private File classesDirectrory;

	/**
	 * The archive configuration to use. See <a
	 * href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver
	 * Reference</a>.
	 */
	@Parameter
	private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

	/**
	 * Whether creating the archive should be forced.
	 */
	@Parameter(property = "archive.forceCreation", defaultValue = "false")
	private boolean forceCreation;

	/**
	 * The current repository/network configuration of Maven.
	 */
	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	private RepositorySystemSession repositorySystemSession;

	/**
	 * Returns the type as defined in plexus components.xml
	 */
	protected abstract String getType();

	/**
	 * Returns the file extension for the archive (e.g. 'jar').
	 */
	protected abstract String getExtension();

	/**
	 * Returns the destination of an {@link Artifact}.
	 * @param artifact the artifact
	 * @return the destination or {@code null} to exclude
	 */
	protected abstract String getArtifactDestination(Artifact artifact);

	/**
	 * Returns the launcher class that will be used.
	 */
	protected abstract String getLauncherClass();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		File archiveFile = createArchive();
		if (this.classifier == null || this.classifier.isEmpty()) {
			this.project.getArtifact().setFile(archiveFile);
		} else {
			this.projectHelper.attachArtifact(this.project, getType(), this.classifier,
					archiveFile);
		}
	}

	private File createArchive() throws MojoExecutionException {
		File archiveFile = getTargetFile();
		MavenArchiver archiver = new MavenArchiver();
		customizeArchiveConfiguration();

		archiver.setArchiver(this.jarArchiver);
		archiver.setOutputFile(archiveFile);
		archiver.getArchiver().setRecompressAddedZips(false);

		try {
			addContent(archiver);
			addLibs(archiver);
			ZipFile zipFile = addLauncherClasses(archiver);
			try {
				archiver.createArchive(this.session, this.project, this.archive);
				return archiveFile;
			} finally {
				zipFile.close();
			}
		} catch (Exception ex) {
			throw new MojoExecutionException("Error assembling archive", ex);
		}
	}

	private File getTargetFile() {
		String classifier = (this.classifier == null ? "" : this.classifier.trim());
		if (classifier.length() > 0 && !classifier.startsWith("-")) {
			classifier = "-" + classifier;
		}
		return new File(this.outputDirectory, this.finalName + classifier + "."
				+ getExtension());
	}

	private void customizeArchiveConfiguration() throws MojoExecutionException {
		this.archive.setForced(this.forceCreation);
		String mainClass = this.mainClass;
		if (mainClass == null) {
			mainClass = this.archive.getManifestEntries().get(MAIN_CLASS_ATTRIBUTE);
		}
		if (mainClass == null) {
			mainClass = MainClassFinder.findMainClass(this.classesDirectrory);
		}
		if (mainClass == null) {
			throw new MojoExecutionException("Unable to find a suitable main class, "
					+ "please add a 'mainClass' property");
		}
		this.archive.getManifestEntries().put(MAIN_CLASS_ATTRIBUTE, getLauncherClass());
		this.archive.getManifestEntries().put(START_CLASS_ATTRIBUTE, mainClass);
	}

	protected void addContent(MavenArchiver archiver) {
		if (this.classesDirectrory.exists()) {
			archiver.getArchiver().addDirectory(this.classesDirectrory,
					getClassesDirectoryPrefix(), getIncludes(), getExcludes());
		}
	}

	protected String getClassesDirectoryPrefix() {
		return "";
	}

	protected final String[] getIncludes() {
		if (this.includes != null && this.includes.length > 0) {
			return this.includes;
		}
		return DEFAULT_INCLUDES;
	}

	protected final String[] getExcludes() {
		if (this.excludes != null && this.excludes.length > 0) {
			return this.excludes;
		}
		return DEFAULT_EXCLUDES;
	}

	private void addLibs(MavenArchiver archiver) {
		for (Artifact artifact : this.project.getArtifacts()) {
			if (artifact.getFile() != null) {
				String dir = getArtifactDestination(artifact);
				if (dir != null) {
					archiver.getArchiver().addFile(artifact.getFile(),
							dir + artifact.getFile().getName());
				}
			}
		}
	}

	private ZipFile addLauncherClasses(MavenArchiver archiver)
			throws MojoExecutionException {
		try {
			List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();
			repositories.addAll(this.project.getRemotePluginRepositories());
			repositories.addAll(this.project.getRemoteProjectRepositories());

			String version = getClass().getPackage().getImplementationVersion();
			DefaultArtifact artifact = new DefaultArtifact(
					"org.springframework.zero:spring-zero-launcher:" + version);
			ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest(
					artifact, repositories, "plugin");
			ArtifactDescriptorResult descriptorResult = this.repositorySystem
					.readArtifactDescriptor(this.repositorySystemSession,
							descriptorRequest);

			ArtifactRequest artifactRequest = new ArtifactRequest();
			artifactRequest.setRepositories(repositories);
			artifactRequest.setArtifact(descriptorResult.getArtifact());
			ArtifactResult artifactResult = this.repositorySystem.resolveArtifact(
					this.repositorySystemSession, artifactRequest);

			if (artifactResult.getArtifact() == null) {
				throw new MojoExecutionException("Unable to resolve launcher classes");
			}
			return addLauncherClasses(archiver, artifactResult.getArtifact().getFile());
		} catch (Exception ex) {
			if (ex instanceof MojoExecutionException) {
				throw (MojoExecutionException) ex;
			}
			throw new MojoExecutionException("Unable to add launcher classes", ex);
		}
	}

	private ZipFile addLauncherClasses(MavenArchiver archiver, File file)
			throws IOException {
		ZipFile zipFile = new ZipFile(file);
		Enumeration<? extends ZipEntry> entries = zipFile.getEntries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			if (!entry.isDirectory() && !entry.getName().startsWith("/META-INF")) {
				ZipResource zipResource = new ZipResource(zipFile, entry);
				archiver.getArchiver().addResource(zipResource, entry.getName(), -1);
			}
		}
		return zipFile;
	}
}
