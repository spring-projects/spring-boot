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

package org.springframework.maven.packaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.zip.ZipEntry;
import org.codehaus.plexus.archiver.zip.ZipFile;
import org.codehaus.plexus.archiver.zip.ZipResource;
import org.codehaus.plexus.util.IOUtil;
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
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ExecutableArchiveMojo extends AbstractMojo {

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
	 * attached. If this is not given, it will merely be written to the output directory
	 * according to the finalName.
	 */
	@Parameter
	private String classifier;

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
	@Parameter(property = "archive.forceCreation", defaultValue = "true")
	private boolean forceCreation;

	/**
	 * The current repository/network configuration of Maven.
	 */
	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	private RepositorySystemSession repositorySystemSession;

	private ExecutableJarHelper jarHelper = new ExecutableJarHelper();

	private ExecutableWarHelper warHelper = new ExecutableWarHelper();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		File archiveFile = createArchive();
		if (this.classifier == null || this.classifier.isEmpty()) {
			this.project.getArtifact().setFile(archiveFile);
		}
		else {
			getLog().info(
					"Attaching archive: " + archiveFile + ", with classifier: "
							+ this.classifier);
			this.projectHelper.attachArtifact(this.project, getType(), this.classifier,
					archiveFile);
		}
	}

	private ArchiveHelper getArchiveHelper() throws MojoExecutionException {
		if (getType().equals("jar")) {
			return this.jarHelper;
		}
		if (getType().equals("war")) {
			return this.warHelper;
		}
		throw new MojoExecutionException("Unsupported packaging type: " + getType());
	}

	private File createArchive() throws MojoExecutionException {
		File archiveFile = getTargetFile();
		MavenArchiver archiver = new MavenArchiver();
		customizeArchiveConfiguration();

		archiver.setArchiver(this.jarArchiver);
		archiver.setOutputFile(archiveFile);
		archiver.getArchiver().setRecompressAddedZips(false);

		try {
			getLog().info("Modifying archive: " + archiveFile);
			copyContent(archiver, this.project.getArtifact().getFile());
			addLibs(archiver);
			ZipFile zipFile = addLauncherClasses(archiver);
			try {
				archiver.createArchive(this.session, this.project, this.archive);
				return archiveFile;
			}
			finally {
				zipFile.close();
			}
		}
		catch (Exception ex) {
			throw new MojoExecutionException("Error assembling archive", ex);
		}
	}

	private String getType() {
		return this.project.getPackaging();
	}

	private String getExtension() {
		return this.project.getPackaging();
	}

	private void copyContent(MavenArchiver archiver, File file) throws IOException {

		FileInputStream input = new FileInputStream(file);
		File original = new File(this.outputDirectory, "original.jar");
		FileOutputStream output = new FileOutputStream(original);
		IOUtil.copy(input, output, 2048);
		input.close();
		output.close();

		ZipFile zipFile = new ZipFile(original);
		Enumeration<? extends ZipEntry> entries = zipFile.getEntries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			// TODO: maybe merge manifest instead of skipping it?
			if (!entry.isDirectory()
					&& !entry.getName().toUpperCase().equals("/META-INF/MANIFEST.MF")) {
				ZipResource zipResource = new ZipResource(zipFile, entry);
				archiver.getArchiver().addResource(zipResource, entry.getName(), -1);
			}
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
		this.archive.getManifestEntries().put(MAIN_CLASS_ATTRIBUTE,
				getArchiveHelper().getLauncherClass());
		this.archive.getManifestEntries().put(START_CLASS_ATTRIBUTE, mainClass);
	}

	private void addLibs(MavenArchiver archiver) throws MojoExecutionException {
		getLog().info("Adding dependencies");
		for (Artifact artifact : this.project.getArtifacts()) {
			if (artifact.getFile() != null) {
				String dir = getArchiveHelper().getArtifactDestination(artifact);
				if (dir != null) {
					getLog().debug("Adding dependency: " + artifact);
					archiver.getArchiver().addFile(artifact.getFile(),
							dir + artifact.getFile().getName());
				}
			}
		}
	}

	private ZipFile addLauncherClasses(MavenArchiver archiver)
			throws MojoExecutionException {
		getLog().info("Adding launcher classes");
		try {
			List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();
			repositories.addAll(this.project.getRemotePluginRepositories());
			repositories.addAll(this.project.getRemoteProjectRepositories());

			String version = getClass().getPackage().getImplementationVersion();
			DefaultArtifact artifact = new DefaultArtifact(
					"org.springframework.zero:spring-launcher:" + version);
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
		}
		catch (Exception ex) {
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
