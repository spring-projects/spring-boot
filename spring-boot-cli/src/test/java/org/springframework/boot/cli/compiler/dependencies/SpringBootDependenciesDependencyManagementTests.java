/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.cli.compiler.dependencies;

import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link SpringBootDependenciesDependencyManagement}
 *
 * @author Andy Wilkinson
 */
public class SpringBootDependenciesDependencyManagementTests {

	private final DependencyManagement dependencyManagement = new SpringBootDependenciesDependencyManagement();

	@Test
	public void springBootVersion() {
		assertThat(this.dependencyManagement.getSpringBootVersion(), is(notNullValue()));
	}

	@Test
	public void find() {
		Dependency dependency = this.dependencyManagement.find("spring-boot");
		assertThat(dependency, is(notNullValue()));
		assertThat(dependency.getGroupId(), is(equalTo("org.springframework.boot")));
		assertThat(dependency.getArtifactId(), is(equalTo("spring-boot")));
	}

	@Test
	public void getDependencies() {
		assertThat(this.dependencyManagement.getDependencies(), is(not(empty())));
	}

}
