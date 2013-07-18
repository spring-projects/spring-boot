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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Abstract base class for MOJOs that work with executable archives.
 * 
 * @author Phillip Webb
 */
public abstract class AbstractExecutableArchiveMojo extends AbstractMojo {

	protected static final String MAIN_CLASS_ATTRIBUTE = "Main-Class";

	private static final Map<String, ArchiveHelper> ARCHIVE_HELPERS;
	static {
		Map<String, ArchiveHelper> helpers = new HashMap<String, ArchiveHelper>();
		helpers.put("jar", new ExecutableJarHelper());
		helpers.put("war", new ExecutableWarHelper());
		ARCHIVE_HELPERS = Collections.unmodifiableMap(helpers);
	}

	/**
	 * The Maven project.
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * Directory containing the classes and resource files that should be packaged into
	 * the archive.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	private File classesDirectrory;

	/**
	 * The name of the main class. If not specified the first compiled class found that
	 * contains a 'main' method will be used.
	 */
	@Parameter
	private String mainClass;

	/**
	 * The archive configuration to use. See <a
	 * href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver
	 * Reference</a>.
	 */
	@Parameter
	private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

	protected final ArchiveHelper getArchiveHelper() throws MojoExecutionException {
		ArchiveHelper helper = ARCHIVE_HELPERS.get(getType());
		if (helper == null) {
			throw new MojoExecutionException("Unsupported packaging type: " + getType());
		}
		return helper;
	}

	protected final String getStartClass() throws MojoExecutionException {
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
		return mainClass;
	}

	protected final MavenProject getProject() {
		return this.project;
	}

	protected final String getType() {
		return this.project.getPackaging();
	}

	protected final String getExtension() {
		return getProject().getPackaging();
	}

	protected final MavenArchiveConfiguration getArchiveConfiguration() {
		return this.archive;
	}

	protected final File getClassesDirectory() {
		return this.classesDirectrory;
	}
}
