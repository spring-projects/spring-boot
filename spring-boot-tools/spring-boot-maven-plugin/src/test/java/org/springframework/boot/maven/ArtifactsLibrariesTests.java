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
import org.apache.maven.model.Dependency;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCallback;
import org.springframework.boot.loader.tools.LibraryScope;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ArtifactsLibraries}.
 * 
 * @author Phillip Webb
 */
public class ArtifactsLibrariesTests {

	@Mock
	private Artifact artifact;

	private Set<Artifact> artifacts;

	private File file = new File(".");

	private ArtifactsLibraries libs;

	@Mock
	private LibraryCallback callback;

	@Captor
	private ArgumentCaptor<Library> libraryCaptor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.artifacts = Collections.singleton(this.artifact);
		this.libs = new ArtifactsLibraries(this.artifacts, null);
		given(this.artifact.getFile()).willReturn(this.file);
	}

	@Test
	public void callbackForJars() throws Exception {
		given(this.artifact.getType()).willReturn("jar");
		given(this.artifact.getScope()).willReturn("compile");
		this.libs.doWithLibraries(this.callback);
		verify(this.callback).library(this.libraryCaptor.capture());
		Library library = this.libraryCaptor.getValue();
		assertThat(library.getFile(), equalTo(this.file));
		assertThat(library.getScope(), equalTo(LibraryScope.COMPILE));
		assertThat(library.isUnpackRequired(), equalTo(false));
	}

	@Test
	public void callbackWithUnpack() throws Exception {
		given(this.artifact.getGroupId()).willReturn("gid");
		given(this.artifact.getArtifactId()).willReturn("aid");
		given(this.artifact.getType()).willReturn("jar");
		given(this.artifact.getScope()).willReturn("compile");
		Dependency unpack = new Dependency();
		unpack.setGroupId("gid");
		unpack.setArtifactId("aid");
		this.libs = new ArtifactsLibraries(this.artifacts, Collections.singleton(unpack));
		this.libs.doWithLibraries(this.callback);
		verify(this.callback).library(this.libraryCaptor.capture());
		assertThat(this.libraryCaptor.getValue().isUnpackRequired(), equalTo(true));
	}
}
