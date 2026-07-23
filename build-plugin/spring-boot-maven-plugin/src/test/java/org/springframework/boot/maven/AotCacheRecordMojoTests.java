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

import java.util.List;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AotCacheRecordMojo}.
 *
 * @author Vasily Pelikh
 */
class AotCacheRecordMojoTests {

	private final Properties properties = new Properties();

	private final MavenProject project = new MavenProject(new Model());

	private final AotCacheRecordMojo mojo = new AotCacheRecordMojo();

	@BeforeEach
	void setup() {
		this.project.getModel().setProperties(this.properties);
		Build build = new Build();
		build.setDirectory("/project/target");
		this.project.getModel().setBuild(build);
		MavenSession session = mock(MavenSession.class);
		given(session.getGoals()).willReturn(List.of("spring-boot:build-image", "test"));
		this.mojo.setLog(mock(Log.class));
		this.mojo.setProject(this.project);
		this.mojo.setSession(session);
	}

	@Test
	void doesNothingWhenPropertyIsAbsent() throws Exception {
		this.mojo.execute();
		assertThat(this.properties).doesNotContainKey("argLine");
	}

	@Test
	void setsArgLineWhenPropertyIsPresent() throws Exception {
		this.mojo.setAotCacheRecord(true);
		this.mojo.execute();
		String argLine = this.properties.getProperty("argLine");
		assertThat(argLine).contains("-XX:AOTCacheOutput=");
	}

	@Test
	void appendsToExistingArgLine() throws Exception {
		this.properties.setProperty("argLine", "-javaagent:mockito.jar");
		this.mojo.setAotCacheRecord(true);
		this.mojo.execute();
		String argLine = this.properties.getProperty("argLine");
		assertThat(argLine).startsWith("-javaagent:mockito.jar");
		assertThat(argLine).contains("-XX:AOTCacheOutput=");
	}

	@Test
	void doesNothingWhenBuildImageGoalIsNotPresent() throws Exception {
		MavenSession session = mock(MavenSession.class);
		given(session.getGoals()).willReturn(List.of("compile", "test"));
		this.mojo.setSession(session);
		this.mojo.setAotCacheRecord(true);
		this.mojo.execute();
		assertThat(this.properties).doesNotContainKey("argLine");
	}

}
