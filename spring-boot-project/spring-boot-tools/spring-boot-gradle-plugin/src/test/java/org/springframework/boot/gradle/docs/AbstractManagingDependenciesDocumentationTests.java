/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.gradle.docs;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the managing dependencies documentation.
 *
 * @author Andy Wilkinson
 * @author Jean-Baptiste Nizet
 */
public abstract class AbstractManagingDependenciesDocumentationTests
		extends AbstractDocumentationTests {

	protected AbstractManagingDependenciesDocumentationTests(DSL dsl) {
		super(dsl);
	}

	@Test
	public void dependenciesExampleEvaluatesSuccessfully() {
		this.gradleBuild
				.script("src/main/gradle/managing-dependencies/dependencies" + extension)
				.build();
	}

	@Test
	public void customManagedVersions() {
		assertThat(this.gradleBuild.script(
				"src/main/gradle/managing-dependencies/custom-version" + extension)
				.build("slf4jVersion").getOutput()).contains("1.7.20");
	}

	@Test
	public void dependencyManagementInIsolation() {
		assertThat(this.gradleBuild
				.script("src/main/gradle/managing-dependencies/configure-bom" + extension)
				.build("dependencyManagement").getOutput())
						.contains("org.springframework.boot:spring-boot-starter ");
	}

}
