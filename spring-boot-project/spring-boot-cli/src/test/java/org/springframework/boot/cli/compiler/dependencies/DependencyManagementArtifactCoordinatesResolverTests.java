/*
 * Copyright 2012-2017 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DependencyManagementArtifactCoordinatesResolver}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class DependencyManagementArtifactCoordinatesResolverTests {

	private DependencyManagement dependencyManagement;

	private DependencyManagementArtifactCoordinatesResolver resolver;

	@Before
	public void setup() {
		this.dependencyManagement = mock(DependencyManagement.class);
		given(this.dependencyManagement.find("a1"))
				.willReturn(new Dependency("g1", "a1", "0"));
		given(this.dependencyManagement.getSpringBootVersion()).willReturn("1");
		this.resolver = new DependencyManagementArtifactCoordinatesResolver(
				this.dependencyManagement);
	}

	@Test
	public void getGroupIdForBootArtifact() {
		assertThat(this.resolver.getGroupId("spring-boot-something"))
				.isEqualTo("org.springframework.boot");
		verify(this.dependencyManagement, never()).find(anyString());
	}

	@Test
	public void getGroupIdFound() {
		assertThat(this.resolver.getGroupId("a1")).isEqualTo("g1");
	}

	@Test
	public void getGroupIdNotFound() {
		assertThat(this.resolver.getGroupId("a2")).isNull();
	}

}
