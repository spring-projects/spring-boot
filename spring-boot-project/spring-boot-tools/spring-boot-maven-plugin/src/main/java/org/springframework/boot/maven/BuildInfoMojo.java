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
import java.util.LinkedHashMap;
import java.util.List;
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
	 * The Maven project.
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * The location of the generated {@code build-info.properties} file.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/build-info.properties")
	private File outputFile;

	/**
	 * The value used for the {@code build.time} property in a form suitable for
	 * {@link Instant#parse(CharSequence)}. Defaults to
	 * {@code project.build.outputTimestamp} or {@code session.request.startTime} if the
	 * former is not set. To disable the {@code build.time} property entirely, use
	 * {@code 'off'} or add it to {@code excludeInfoProperties}.
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

	/**
	 * Properties that should be excluded {@code build-info.properties} file. Can be used
	 * to exclude the standard {@code group}, {@code artifact}, {@code name},
	 * {@code version} or {@code time} properties as well as items from
	 * {@code additionalProperties}.
	 */
	@Parameter
	private List<String> excludeInfoProperties;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			ProjectDetails details = getProjectDetails();
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

	private ProjectDetails getProjectDetails() {
		String group = getIfNotExcluded("group", this.project.getGroupId());
		String artifact = getIfNotExcluded("artifact", this.project.getArtifactId());
		String version = getIfNotExcluded("version", this.project.getVersion());
		String name = getIfNotExcluded("name", this.project.getName());
		Instant time = getIfNotExcluded("time", getBuildTime());
		Map<String, String> additionalProperties = applyExclusions(this.additionalProperties);
		return new ProjectDetails(group, artifact, version, name, time, additionalProperties);
	}

	private <T> T getIfNotExcluded(String name, T value) {
		return (this.excludeInfoProperties == null || !this.excludeInfoProperties.contains(name)) ? value : null;
	}

	private Map<String, String> applyExclusions(Map<String, String> source) {
		if (source == null || this.excludeInfoProperties == null) {
			return source;
		}
		Map<String, String> result = new LinkedHashMap<>(source);
		this.excludeInfoProperties.forEach(result::remove);
		return result;
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
