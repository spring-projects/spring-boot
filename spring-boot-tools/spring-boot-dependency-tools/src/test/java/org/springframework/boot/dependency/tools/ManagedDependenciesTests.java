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

package org.springframework.boot.dependency.tools;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ManagedDependencies}.
 *
 * @author Phillip Webb
 */
public class ManagedDependenciesTests {

	private ManagedDependencies managedDependencies;

	private Dependencies delegate;

	@Before
	public void setup() {
		this.delegate = mock(Dependencies.class);
		this.managedDependencies = new ManagedDependencies(this.delegate) {
		};
	}

	@Test
	@Deprecated
	public void getVersion() throws Exception {
		this.managedDependencies.getVersion();
		verify(this.delegate).find("org.springframework.boot", "spring-boot");
	}

	@Test
	public void getSpringBootVersion() throws Exception {
		this.managedDependencies.getSpringBootVersion();
		verify(this.delegate).find("org.springframework.boot", "spring-boot");
	}

	@Test
	public void findGroupIdArtifactId() throws Exception {
		this.managedDependencies.find("groupId", "artifactId");
		verify(this.delegate).find("groupId", "artifactId");
	}

	@Test
	public void findArtifactId() throws Exception {
		this.managedDependencies.find("artifactId");
		verify(this.delegate).find("artifactId");
	}

	@Test
	public void iterator() throws Exception {
		this.managedDependencies.iterator();
		verify(this.delegate).iterator();
	}

}
