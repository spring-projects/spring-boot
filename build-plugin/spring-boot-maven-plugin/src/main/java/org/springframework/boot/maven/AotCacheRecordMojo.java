/*
 * Copyright 2012-present the original author or authors.
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

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Records the Leyden AOT cache for integration tests.
 *
 * <p>
 * This goal injects the JVM arguments required to record the Leyden AOT cache into the
 * {@code maven-surefire-plugin} {@code argLine}. The cache file is written by the JVM on
 * clean exit.
 *
 * <p>
 * Enable by setting the {@code spring-boot.aot-cache-record} property:
 *
 * <pre>{@code
 * &lt;properties&gt;
 *     &lt;spring-boot.aot-cache-record&gt;true&lt;/spring-boot.aot-cache-record&gt;
 * &lt;/properties&gt;
 * }</pre>
 *
 * @author Vasily Pelikh
 * @since 4.2.0
 */
@Mojo(name = "aot-cache-record", defaultPhase = LifecyclePhase.INITIALIZE)
public class AotCacheRecordMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	@SuppressWarnings("NullAway.Init")
	private MavenProject project;

	@Parameter(defaultValue = "${session}", readonly = true)
	@SuppressWarnings("NullAway.Init")
	private MavenSession session;

	@Parameter(property = "spring-boot.aot-cache-record", defaultValue = "false")
	private boolean aotCacheRecord;

	void setAotCacheRecord(boolean aotCacheRecord) {
		this.aotCacheRecord = aotCacheRecord;
	}

	void setProject(MavenProject project) {
		this.project = project;
	}

	void setSession(MavenSession session) {
		this.session = session;
	}

	@Override
	public void execute() throws MojoExecutionException {
		if (!this.aotCacheRecord) {
			getLog().debug("AOT cache recording is not enabled (spring-boot.aot-cache-record is not true).");
			return;
		}
		if (!hasBuildImageGoal()) {
			getLog().warn("AOT cache recording enabled but spring-boot:build-image goal is not in the execution plan. "
					+ "Cache recording will be skipped.");
			return;
		}
		String outputPath = resolveOutputPath();
		String argLine = buildArgLine(outputPath);
		appendToProjectArgLine(argLine);
		getLog().info("Configured test AOT cache recording with argLine: " + argLine);
	}

	private boolean hasBuildImageGoal() {
		List<String> goals = (this.session != null && this.session.getGoals() != null) ? this.session.getGoals()
				: Collections.emptyList();
		return goals.stream()
			.anyMatch((g) -> g.equals("spring-boot:build-image") || g.equals("spring-boot:build-image-no-fork"));
	}

	private String resolveOutputPath() {
		return Path.of(this.project.getBuild().getDirectory(), "aot-cache")
			.resolve("application.aot")
			.toAbsolutePath()
			.toString();
	}

	private String buildArgLine(String outputPath) {
		return "-XX:AOTCacheOutput=" + outputPath;
	}

	private void appendToProjectArgLine(String additionalArgs) {
		String existing = this.project.getProperties().getProperty("argLine", "").trim();
		String combined = existing.isEmpty() ? additionalArgs : existing + " " + additionalArgs;
		this.project.getProperties().put("argLine", combined);
	}

}
