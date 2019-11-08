/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.gradle.docs;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.gradle.junit.GradleMultiDslExtension;
import org.springframework.boot.gradle.testkit.Dsl;
import org.springframework.boot.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumingThat;

/**
 * Tests for the managing dependencies documentation.
 *
 * @author Andy Wilkinson
 * @author Jean-Baptiste Nizet
 */
@ExtendWith(GradleMultiDslExtension.class)
public class ManagingDependenciesDocumentationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	public void dependenciesExampleEvaluatesSuccessfully() {
		this.gradleBuild.script("src/main/gradle/managing-dependencies/dependencies").build();
	}

	@TestTemplate
	public void customManagedVersions() {
		assertThat(this.gradleBuild.script("src/main/gradle/managing-dependencies/custom-version").build("slf4jVersion")
				.getOutput()).contains("1.7.20");
	}

	@TestTemplate
	public void dependencyManagementInIsolation() {
		assertThat(this.gradleBuild.script("src/main/gradle/managing-dependencies/configure-bom")
				.build("dependencyManagement").getOutput()).contains("org.springframework.boot:spring-boot-starter ");
	}

	@TestTemplate
	public void dependencyManagementInIsolationWithPluginsBlock() {
		assumingThat(this.gradleBuild.getDsl() == Dsl.KOTLIN,
				() -> assertThat(
						this.gradleBuild.script("src/main/gradle/managing-dependencies/configure-bom-with-plugins")
								.build("dependencyManagement").getOutput())
										.contains("org.springframework.boot:spring-boot-starter TEST-SNAPSHOT"));
	}

}
