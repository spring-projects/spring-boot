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
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;

import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
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
 * MOJO that can can be used to repackage existing JAR and WAR archives so that they can
 * be executed from the command line using {@literal java -jar}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ExecutableArchiveMojo extends AbstractExecutableArchiveMojo {

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
	 * Classifier to add to the artifact generated. If given, the artifact will be
	 * attached. If this is not given, it will merely be written to the output directory
	 * according to the finalName.
	 */
	@Parameter
	private String classifier;

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

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		File archiveFile = createArchive();
		if (this.classifier == null || this.classifier.isEmpty()) {
			getProject().getArtifact().setFile(archiveFile);
		}
		else {
			getLog().info(
					"Attaching archive: " + archiveFile + ", with classifier: "
							+ this.classifier);
			this.projectHelper.attachArtifact(getProject(), getType(), this.classifier,
					archiveFile);
		}
	}

	private File createArchive() throws MojoExecutionException {
		File archiveFile = getTargetFile();
		MavenArchiver archiver = new MavenArchiver();

		archiver.setArchiver(this.jarArchiver);
		archiver.setOutputFile(archiveFile);
		archiver.getArchiver().setRecompressAddedZips(false);

		try {
			getLog().info("Modifying archive: " + archiveFile);
			Manifest manifest = copyContent(archiver, getProject().getArtifact()
					.getFile());
			customizeArchiveConfiguration(manifest);
			addLibs(archiver);
			ZipFile zipFile = addLauncherClasses(archiver);
			try {
				archiver.createArchive(this.session, getProject(),
						getArchiveConfiguration());
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

	private Manifest copyContent(MavenArchiver archiver, File file) throws IOException {

		FileInputStream input = new FileInputStream(file);
		File original = new File(this.outputDirectory, "original.jar");
		FileOutputStream output = new FileOutputStream(original);
		IOUtil.copy(input, output, 2048);
		input.close();
		output.close();

		Manifest manifest = new Manifest();
		ZipFile zipFile = new ZipFile(original);
		Enumeration<? extends ZipEntry> entries = zipFile.getEntries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			if (!entry.isDirectory()) {
				ZipResource zipResource = new ZipResource(zipFile, entry);
				getLog().debug("Copying resource: " + entry.getName());
				if (!entry.getName().toUpperCase().equals("META-INF/MANIFEST.MF")) {
					archiver.getArchiver().addResource(zipResource, entry.getName(), -1);
				}
				else {
					getLog().info("Found existing manifest");
					manifest = new Manifest(zipResource.getContents());
				}
			}
		}

		return manifest;

	}

	private File getTargetFile() {
		String classifier = (this.classifier == null ? "" : this.classifier.trim());
		if (classifier.length() > 0 && !classifier.startsWith("-")) {
			classifier = "-" + classifier;
		}
		return new File(this.outputDirectory, this.finalName + classifier + "."
				+ getExtension());
	}

	private void customizeArchiveConfiguration(Manifest manifest)
			throws MojoExecutionException {
		getArchiveConfiguration().setForced(this.forceCreation);

		Attributes attributes = manifest.getMainAttributes();
		for (Object name : attributes.keySet()) {
			String value = attributes.getValue((Name) name);
			getLog().debug("Existing manifest entry: " + name + "=" + value);
			getArchiveConfiguration().addManifestEntry(name.toString(), value);
		}

		String startClass = getStartClass();
		getArchiveConfiguration().addManifestEntry(MAIN_CLASS_ATTRIBUTE,
				getArchiveHelper().getLauncherClass());
		getArchiveConfiguration().addManifestEntry(START_CLASS_ATTRIBUTE, startClass);
	}

	private void addLibs(MavenArchiver archiver) throws MojoExecutionException {
		getLog().info("Adding dependencies");
		ArchiveHelper archiveHelper = getArchiveHelper();
		for (Artifact artifact : getProject().getArtifacts()) {
			if (artifact.getFile() != null) {
				String dir = archiveHelper.getArtifactDestination(artifact);
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
			repositories.addAll(getProject().getRemotePluginRepositories());
			repositories.addAll(getProject().getRemoteProjectRepositories());

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
