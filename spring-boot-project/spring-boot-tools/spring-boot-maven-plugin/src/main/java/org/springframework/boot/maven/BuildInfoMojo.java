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
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

import org.springframework.boot.loader.tools.BuildPropertiesWriter;
import org.springframework.boot.loader.tools.BuildPropertiesWriter.NullAdditionalPropertyValueException;
import org.springframework.boot.loader.tools.BuildPropertiesWriter.ProjectDetails;

/**
 * Generate a {@code build-info.properties} file based on the content of the current
 * {@link MavenProject}.
 *
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 1.4.0
 */
@Mojo(name = "build-info", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class BuildInfoMojo extends AbstractMojo {

	@Component
	private BuildContext buildContext;

	/**
	 * The Maven session.
	 */
	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession session;

	/**
	 * The location of the generated {@code build-info.properties} file.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/build-info.properties")
	private File outputFile;

	/**
	 * The value used for the {@code build.group} property. Defaults to
	 * {@code project.groupId}. To disable the {@code build.group} property entirely, use
	 * {@code 'off'}.
	 * @since 2.6.0
	 */
	@Parameter(defaultValue = "${project.groupId}")
	private String group;

	/**
	 * The value used for the {@code build.artifact} property. Defaults to
	 * {@code project.artifactId}. To disable the {@code build.artifact} property
	 * entirely, use {@code 'off'}.
	 * @since 2.6.0
	 */
	@Parameter(defaultValue = "${project.artifactId}")
	private String artifact;

	/**
	 * The value used for the {@code build.version} property. Defaults to
	 * {@code project.version}. To disable the {@code build.version} property entirely,
	 * use {@code 'off'}.
	 * @since 2.6.0
	 */
	@Parameter(defaultValue = "${project.version}")
	private String version;

	/**
	 * The value used for the {@code build.name} property. Defaults to
	 * {@code project.name}. To disable the {@code build.name} property entirely, use
	 * {@code 'off'}.
	 * @since 2.6.0
	 */
	@Parameter(defaultValue = "${project.name}")
	private String name;

	/**
	 * The value used for the {@code build.time} property in a form suitable for
	 * {@link Instant#parse(CharSequence)}. Defaults to
	 * {@code project.build.outputTimestamp} or {@code session.request.startTime} if the
	 * former is not set. To disable the {@code build.time} property entirely, use
	 * {@code 'off'}.
	 * @since 2.2.0
	 */
	@Parameter(defaultValue = "${project.build.outputTimestamp}")
	private String time;

	/**
	 * Additional properties to store in the {@code build-info.properties} file. Each
	 * entry is prefixed by {@code build.} in the generated {@code build-info.properties}.
	 */
	@Parameter
	private Map<String, String> additionalProperties;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			ProjectDetails details = new ProjectDetails(getGroup(), getArtifact(), getVersion(), getName(),
					getBuildTime(), this.additionalProperties);
			new BuildPropertiesWriter(this.outputFile).writeBuildProperties(details);
			this.buildContext.refresh(this.outputFile);
		}
		catch (NullAdditionalPropertyValueException ex) {
			throw new MojoFailureException("Failed to generate build-info.properties. " + ex.getMessage(), ex);
		}
		catch (Exception ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}

	private String getGroup() {
		if ("off".equalsIgnoreCase(this.group)) {
			return null;
		}
		return this.group;
	}

	private String getArtifact() {
		if ("off".equalsIgnoreCase(this.artifact)) {
			return null;
		}
		return this.artifact;
	}

	private String getVersion() {
		if ("off".equalsIgnoreCase(this.version)) {
			return null;
		}
		return this.version;
	}

	private String getName() {
		if ("off".equalsIgnoreCase(this.name)) {
			return null;
		}
		return this.name;
	}

	private Instant getBuildTime() {
		if (this.time == null || this.time.isEmpty()) {
			Date startTime = this.session.getRequest().getStartTime();
			return (startTime != null) ? startTime.toInstant() : Instant.now();
		}
		if ("off".equalsIgnoreCase(this.time)) {
			return null;
		}
		return Instant.parse(this.time);
	}

}
