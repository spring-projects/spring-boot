/*
 * Copyright 2012-2023 the original author or authors.
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
import java.io.IOException;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Integration tests for the Maven plugin's {@code test-run} goal.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(MavenBuildExtension.class)
class TestRunIntegrationTests {

	@TestTemplate
	void whenTheTestRunGoalIsExecutedTheApplicationIsRunWithTestAndMainClassesAndTestClasspath(MavenBuild mavenBuild) {
		mavenBuild.project("test-run")
			.goals("spring-boot:test-run", "-X")
			.execute((project) -> assertThat(buildLog(project))
				.contains("Main class name = org.test.TestSampleApplication")
				.contains("1. " + canonicalPathOf(project, "target/test-classes"))
				.contains("2. " + canonicalPathOf(project, "target/classes"))
				.containsPattern("3\\. .*spring-core")
				.containsPattern("4\\. .*spring-jcl"));
	}

	private String canonicalPathOf(File project, String path) throws IOException {
		return new File(project, path).getCanonicalPath();
	}

	private String buildLog(File project) {
		return contentOf(new File(project, "target/build.log"));
	}

}
