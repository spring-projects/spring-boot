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

package org.springframework.boot.maven;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.loader.tools.LibraryCallback;
import org.springframework.boot.loader.tools.LibraryScope;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link ArtifactsLibraries}.
 * 
 * @author Phillip Webb
 */
public class ArtifactsLibrariesTest {

	@Mock
	private Artifact artifact;

	private Set<Artifact> artifacts;

	private File file = new File(".");

	private ArtifactsLibraries libs;

	@Mock
	private LibraryCallback callback;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.artifacts = Collections.singleton(this.artifact);
		this.libs = new ArtifactsLibraries(this.artifacts);
		given(this.artifact.getFile()).willReturn(this.file);
	}

	@Test
	public void callbackForJars() throws Exception {
		given(this.artifact.getType()).willReturn("jar");
		given(this.artifact.getScope()).willReturn("compile");
		this.libs.doWithLibraries(this.callback);
		verify(this.callback).library(this.file, LibraryScope.COMPILE);
	}

	@Test
	public void doesNotIncludePoms() throws Exception {
		given(this.artifact.getType()).willReturn("pom");
		given(this.artifact.getScope()).willReturn("compile");
		this.libs.doWithLibraries(this.callback);
		verifyZeroInteractions(this.callback);
	}

}
