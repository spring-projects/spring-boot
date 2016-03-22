/*
 * Copyright 2012-2016 the original author or authors.
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Generate a {@code build.properties} file based the content of the current
 * {@link MavenProject}.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@Mojo(name = "build-info", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class BuildInfoMojo extends AbstractMojo {

	/**
	 * The Maven project.
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * The location of the generated build.properties.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/boot/build.properties")
	private File outputFile;

	/**
	 * Additional properties to store in the build.properties. Each entry is prefixed by
	 * {@code build.} in the generated build.properties.
	 */
	@Parameter
	private Map<String, String> additionalProperties;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Properties properties = createBuildInfo();
		try {
			createFileIfNecessary(this.outputFile);
			FileOutputStream outputStream = new FileOutputStream(this.outputFile);
			try {
				properties.store(outputStream, "Properties");
			}
			finally {
				closeQuietly(outputStream);
			}
		}
		catch (FileNotFoundException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
		catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void closeQuietly(OutputStream outputStream) {
		try {
			outputStream.close();
		}
		catch (IOException ex) {
			getLog().error("Error closing FileOutputStream: " + outputStream);
		}
	}

	protected Properties createBuildInfo() {
		Properties properties = new Properties();
		properties.put("build.group", this.project.getGroupId());
		properties.put("build.artifact", this.project.getArtifactId());
		properties.put("build.name", this.project.getName());
		properties.put("build.version", this.project.getVersion());
		properties.put("build.time", formatDate(new Date()));
		if (this.additionalProperties != null) {
			for (Map.Entry<String, String> entry : this.additionalProperties.entrySet()) {
				properties.put("build." + entry.getKey(), entry.getValue());
			}
		}
		return properties;
	}

	private String formatDate(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		return sdf.format(date);
	}

	private void createFileIfNecessary(File file)
			throws MojoExecutionException, IOException {
		if (file.exists()) {
			return;
		}
		File parent = file.getParentFile();
		if (!parent.exists() && !parent.mkdirs()) {
			throw new MojoExecutionException("Cannot create parent directory for '"
					+ this.outputFile.getAbsolutePath() + "'");
		}
		if (!file.createNewFile()) {
			throw new MojoExecutionException("Cannot create target file '"
					+ this.outputFile.getAbsolutePath() + "'");
		}
	}

}
