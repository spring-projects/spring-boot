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

package org.springframework.boot.maven;

import java.io.File;
import java.io.IOException;

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
import org.springframework.boot.loader.tools.Layout;
import org.springframework.boot.loader.tools.Layouts;
import org.springframework.boot.loader.tools.Libraries;
import org.springframework.boot.loader.tools.Repackager;

/**
 * MOJO that can can be used to repackage existing JAR and WAR archives so that they can
 * be executed from the command line using {@literal java -jar}. With
 * <code>layout=NONE</code> can also be used simply to package a JAR with nested
 * dependencies (and no main class, so not executable).
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
@Mojo(name = "repackage", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class RepackageMojo extends AbstractMojo {

	/**
	 * The Maven project.
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * Maven project helper utils.
	 */
	@Component
	private MavenProjectHelper projectHelper;

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
	 * The name of the main class. If not specified the first compiled class found that
	 * contains a 'main' method will be used.
	 */
	@Parameter
	private String mainClass;

	/**
	 * The layout to use (JAR, WAR, ZIP, DIR, NONE) in case it cannot be inferred.
	 */
	@Parameter
	private LayoutType layout;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		File source = this.project.getArtifact().getFile();
		File target = getTargetFile();
		Repackager repackager = new Repackager(source);
		repackager.setMainClass(this.mainClass);
		if (this.layout != null) {
			getLog().info("Layout: " + this.layout);
			repackager.setLayout(this.layout.layout());
		}
		Libraries libraries = new ArtifactsLibraries(this.project.getArtifacts());
		try {
			repackager.repackage(target, libraries);
		}
		catch (IOException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
		if (!source.equals(target)) {
			getLog().info(
					"Attaching archive: " + target + ", with classifier: "
							+ this.classifier);
			this.projectHelper.attachArtifact(this.project, this.project.getPackaging(),
					this.classifier, target);
		}
	}

	private File getTargetFile() {
		String classifier = (this.classifier == null ? "" : this.classifier.trim());
		if (classifier.length() > 0 && !classifier.startsWith("-")) {
			classifier = "-" + classifier;
		}
		return new File(this.outputDirectory, this.finalName + classifier + "."
				+ this.project.getPackaging());
	}

	public static enum LayoutType {
		JAR(new Layouts.Jar()), WAR(new Layouts.War()), ZIP(new Layouts.Expanded()), DIR(
				new Layouts.Expanded()), NONE(new Layouts.None());
		private Layout layout;

		public Layout layout() {
			return this.layout;
		}

		private LayoutType(Layout layout) {
			this.layout = layout;
		}
	}

}
