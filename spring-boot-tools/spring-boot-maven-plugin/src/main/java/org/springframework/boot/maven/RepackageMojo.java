/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;

import org.springframework.boot.loader.tools.DefaultLaunchScript;
import org.springframework.boot.loader.tools.LaunchScript;
import org.springframework.boot.loader.tools.Layout;
import org.springframework.boot.loader.tools.Layouts;
import org.springframework.boot.loader.tools.Libraries;
import org.springframework.boot.loader.tools.Repackager;

/**
 * Repackages existing JAR and WAR archives so that they can be executed from the command
 * line using {@literal java -jar}. With <code>layout=NONE</code> can also be used simply
 * to package a JAR with nested dependencies (and no main class, so not executable).
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 */
@Mojo(name = "repackage", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class RepackageMojo extends AbstractDependencyFilterMojo {

	private static final long FIND_WARNING_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

	/**
	 * The Maven project.
	 * @since 1.0
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * Maven project helper utils.
	 * @since 1.0
	 */
	@Component
	private MavenProjectHelper projectHelper;

	/**
	 * Directory containing the generated archive.
	 * @since 1.0
	 */
	@Parameter(defaultValue = "${project.build.directory}", required = true)
	private File outputDirectory;

	/**
	 * Name of the generated archive.
	 * @since 1.0
	 */
	@Parameter(defaultValue = "${project.build.finalName}", required = true)
	private String finalName;

	/**
	 * Skip the execution.
	 * @since 1.2
	 */
	@Parameter(property = "skip", defaultValue = "false")
	private boolean skip;

	/**
	 * Classifier to add to the artifact generated. If given, the artifact will be
	 * attached. If this is not given, it will merely be written to the output directory
	 * according to the finalName. Attaching the artifact allows to deploy it alongside to
	 * the original one, see <a href=
	 * "http://maven.apache.org/plugins/maven-deploy-plugin/examples/deploying-with-classifiers.html"
	 * > the maven documentation for more details</a>.
	 * @since 1.0
	 */
	@Parameter
	private String classifier;

	/**
	 * The name of the main class. If not specified the first compiled class found that
	 * contains a 'main' method will be used.
	 * @since 1.0
	 */
	@Parameter
	private String mainClass;

	/**
	 * The type of archive (which corresponds to how the dependencies are laid out inside
	 * it). Possible values are JAR, WAR, ZIP, DIR, NONE. Defaults to a guess based on the
	 * archive type.
	 * @since 1.0
	 */
	@Parameter
	private LayoutType layout;

	/**
	 * A list of the libraries that must be unpacked from fat jars in order to run.
	 * Specify each library as a <code>&lt;dependency&gt;</code> with a
	 * <code>&lt;groupId&gt;</code> and a <code>&lt;artifactId&gt;</code> and they will be
	 * unpacked at runtime in <code>$TMPDIR/spring-boot-libs</code>.
	 * @since 1.1
	 */
	@Parameter
	private List<Dependency> requiresUnpack;

	/**
	 * Make a fully executable jar for *nix machines by prepending a launch script to the
	 * jar.
	 * @since 1.3
	 */
	@Parameter(defaultValue = "false")
	private boolean executable;

	/**
	 * The embedded launch script to prepend to the front of the jar if it is fully
	 * executable. If not specified the 'Spring Boot' default script will be used.
	 * @since 1.3
	 */
	@Parameter
	private File embeddedLaunchScript;

	/**
	 * Properties that should be expanded in the embedded launch script.
	 * @since 1.3
	 */
	@Parameter
	private Properties embeddedLaunchScriptProperties;

	/**
	 * Exclude Spring Boot devtools.
	 * @since 1.3
	 */
	@Parameter(defaultValue = "false")
	private boolean excludeDevtools;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.project.getPackaging().equals("pom")) {
			getLog().debug("repackage goal could not be applied to pom project.");
			return;
		}
		if (this.skip) {
			getLog().debug("skipping repackaging as per configuration.");
			return;
		}

		File source = this.project.getArtifact().getFile();
		File target = getTargetFile();
		Repackager repackager = new Repackager(source) {
			@Override
			protected String findMainMethod(JarFile source) throws IOException {
				long startTime = System.currentTimeMillis();
				try {
					return super.findMainMethod(source);
				}
				finally {
					long duration = System.currentTimeMillis() - startTime;
					if (duration > FIND_WARNING_TIMEOUT) {
						getLog().warn("Searching for the main-class is taking some time, "
								+ "consider using the mainClass configuration "
								+ "parameter");
					}
				}
			}
		};
		repackager.setMainClass(this.mainClass);
		if (this.layout != null) {
			getLog().info("Layout: " + this.layout);
			repackager.setLayout(this.layout.layout());
		}

		Set<Artifact> artifacts = filterDependencies(this.project.getArtifacts(),
				getFilters(getAdditionalFilters()));

		Libraries libraries = new ArtifactsLibraries(artifacts, this.requiresUnpack,
				getLog());
		try {
			LaunchScript launchScript = getLaunchScript();
			repackager.repackage(target, libraries, launchScript);
		}
		catch (IOException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
		if (this.classifier != null) {
			getLog().info("Attaching archive: " + target + ", with classifier: "
					+ this.classifier);
			this.projectHelper.attachArtifact(this.project, this.project.getPackaging(),
					this.classifier, target);
		}
		else if (!source.equals(target)) {
			this.project.getArtifact().setFile(target);
			getLog().info("Replacing main artifact " + source + " to " + target);
		}
	}

	private ArtifactsFilter[] getAdditionalFilters() {
		if (this.excludeDevtools) {
			Exclude exclude = new Exclude();
			exclude.setGroupId("org.springframework.boot");
			exclude.setArtifactId("spring-boot-devtools");
			ExcludeFilter filter = new ExcludeFilter(exclude);
			return new ArtifactsFilter[] { filter };
		}
		return new ArtifactsFilter[] {};
	}

	private File getTargetFile() {
		String classifier = (this.classifier == null ? "" : this.classifier.trim());
		if (classifier.length() > 0 && !classifier.startsWith("-")) {
			classifier = "-" + classifier;
		}
		if (!this.outputDirectory.exists()) {
			this.outputDirectory.mkdirs();
		}
		return new File(this.outputDirectory, this.finalName + classifier + "."
				+ this.project.getArtifact().getArtifactHandler().getExtension());
	}

	private LaunchScript getLaunchScript() throws IOException {
		if (this.executable || this.embeddedLaunchScript != null) {
			return new DefaultLaunchScript(this.embeddedLaunchScript,
					buildLaunchScriptProperties());
		}
		return null;
	}

	private Properties buildLaunchScriptProperties() {
		Properties properties = new Properties();
		if (this.embeddedLaunchScriptProperties != null) {
			properties.putAll(this.embeddedLaunchScriptProperties);
		}
		putIfMissing(properties, "initInfoProvides", this.project.getArtifactId());
		putIfMissing(properties, "initInfoShortDescription", this.project.getName(),
				this.project.getArtifactId());
		putIfMissing(properties, "initInfoDescription", this.project.getDescription(),
				this.project.getName(), this.project.getArtifactId());
		return properties;
	}

	private void putIfMissing(Properties properties, String key,
			String... valueCandidates) {
		if (!properties.containsKey(key)) {
			for (String candidate : valueCandidates) {
				if (candidate != null && candidate.length() > 0) {
					properties.put(key, candidate);
					return;
				}
			}
		}
	}

	/**
	 * Archive layout types.
	 */
	public enum LayoutType {

		/**
		 * Jar Layout.
		 */
		JAR(new Layouts.Jar()),

		/**
		 * War Layout.
		 */
		WAR(new Layouts.War()),

		/**
		 * Zip Layout.
		 */
		ZIP(new Layouts.Expanded()),

		/**
		 * Dir Layout.
		 */
		DIR(new Layouts.Expanded()),

		/**
		 * Module Layout.
		 */
		MODULE(new Layouts.Module()),

		/**
		 * No Layout.
		 */
		NONE(new Layouts.None());

		private final Layout layout;

		public Layout layout() {
			return this.layout;
		}

		LayoutType(Layout layout) {
			this.layout = layout;
		}
	}

}
