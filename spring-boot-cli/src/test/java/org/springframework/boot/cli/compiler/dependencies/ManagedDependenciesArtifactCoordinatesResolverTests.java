/*
 * Copyright 2012-2014 the original author or authors.
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
import org.springframework.boot.dependency.tools.Dependency;
import org.springframework.boot.dependency.tools.ManagedDependencies;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ManagedDependenciesArtifactCoordinatesResolver}.
 * 
 * @author Phillip Webb
 */
public class ManagedDependenciesArtifactCoordinatesResolverTests {

	private ManagedDependencies dependencies;

	private ManagedDependenciesArtifactCoordinatesResolver resolver;

	@Before
	public void setup() {
		this.dependencies = mock(ManagedDependencies.class);
		given(this.dependencies.find("a1")).willReturn(new Dependency("g1", "a1", "0"));
		given(this.dependencies.getVersion()).willReturn("1");
		this.resolver = new ManagedDependenciesArtifactCoordinatesResolver(
				this.dependencies);
	}

	@Test
	public void getGroupIdForBootArtifact() throws Exception {
		assertThat(this.resolver.getGroupId("spring-boot-something"),
				equalTo("org.springframework.boot"));
		verify(this.dependencies, never()).find(anyString());
	}

	@Test
	public void getGroupIdFound() throws Exception {
		assertThat(this.resolver.getGroupId("a1"), equalTo("g1"));
	}

	@Test
	public void getGroupIdNotFound() throws Exception {
		assertThat(this.resolver.getGroupId("a2"), nullValue());
	}

	@Test
	public void getVersionForBootArtifact() throws Exception {
		assertThat(this.resolver.getVersion("spring-boot-something"), equalTo("1"));
		verify(this.dependencies, never()).find(anyString());
	}

	@Test
	public void getVersionFound() throws Exception {
		assertThat(this.resolver.getVersion("a1"), equalTo("0"));
	}

	@Test
	public void getVersionNotFound() throws Exception {
		assertThat(this.resolver.getVersion("a2"), nullValue());
	}

}
