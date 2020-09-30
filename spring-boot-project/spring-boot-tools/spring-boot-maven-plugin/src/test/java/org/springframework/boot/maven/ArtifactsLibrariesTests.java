/*
 * Copyright 2012-2020 the original author or authors.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCallback;
import org.springframework.boot.loader.tools.LibraryScope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ArtifactsLibraries}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class ArtifactsLibrariesTests {

	@Mock
	private Artifact artifact;

	@Mock
	private ArtifactHandler artifactHandler;

	private Set<Artifact> artifacts;

	private File file = new File(".");

	private ArtifactsLibraries libs;

	@Mock
	private LibraryCallback callback;

	@Captor
	private ArgumentCaptor<Library> libraryCaptor;

	@BeforeEach
	void setup() {
		this.artifacts = Collections.singleton(this.artifact);
		this.libs = new ArtifactsLibraries(this.artifacts, null, mock(Log.class));
		given(this.artifactHandler.getExtension()).willReturn("jar");
	}

	@Test
	void callbackForJars() throws Exception {
		given(this.artifact.getFile()).willReturn(this.file);
		given(this.artifact.getArtifactHandler()).willReturn(this.artifactHandler);
		given(this.artifact.getScope()).willReturn("compile");
		this.libs.doWithLibraries(this.callback);
		verify(this.callback).library(this.libraryCaptor.capture());
		Library library = this.libraryCaptor.getValue();
		assertThat(library.getFile()).isEqualTo(this.file);
		assertThat(library.getScope()).isEqualTo(LibraryScope.COMPILE);
		assertThat(library.isUnpackRequired()).isFalse();
	}

	@Test
	void callbackWithUnpack() throws Exception {
		given(this.artifact.getFile()).willReturn(this.file);
		given(this.artifact.getArtifactHandler()).willReturn(this.artifactHandler);
		given(this.artifact.getGroupId()).willReturn("gid");
		given(this.artifact.getArtifactId()).willReturn("aid");
		given(this.artifact.getScope()).willReturn("compile");
		Dependency unpack = new Dependency();
		unpack.setGroupId("gid");
		unpack.setArtifactId("aid");
		this.libs = new ArtifactsLibraries(this.artifacts, Collections.singleton(unpack), mock(Log.class));
		this.libs.doWithLibraries(this.callback);
		verify(this.callback).library(this.libraryCaptor.capture());
		assertThat(this.libraryCaptor.getValue().isUnpackRequired()).isTrue();
	}

	@Test
	void renamesDuplicates() throws Exception {
		Artifact artifact1 = mock(Artifact.class);
		Artifact artifact2 = mock(Artifact.class);
		given(artifact1.getScope()).willReturn("compile");
		given(artifact1.getGroupId()).willReturn("g1");
		given(artifact1.getArtifactId()).willReturn("artifact");
		given(artifact1.getBaseVersion()).willReturn("1.0");
		given(artifact1.getFile()).willReturn(new File("a"));
		given(artifact1.getArtifactHandler()).willReturn(this.artifactHandler);
		given(artifact2.getScope()).willReturn("compile");
		given(artifact2.getGroupId()).willReturn("g2");
		given(artifact2.getArtifactId()).willReturn("artifact");
		given(artifact2.getBaseVersion()).willReturn("1.0");
		given(artifact2.getFile()).willReturn(new File("a"));
		given(artifact2.getArtifactHandler()).willReturn(this.artifactHandler);
		this.artifacts = new LinkedHashSet<>(Arrays.asList(artifact1, artifact2));
		this.libs = new ArtifactsLibraries(this.artifacts, null, mock(Log.class));
		this.libs.doWithLibraries(this.callback);
		verify(this.callback, times(2)).library(this.libraryCaptor.capture());
		assertThat(this.libraryCaptor.getAllValues().get(0).getName()).isEqualTo("g1-artifact-1.0.jar");
		assertThat(this.libraryCaptor.getAllValues().get(1).getName()).isEqualTo("g2-artifact-1.0.jar");
	}

	@Test
	void libraryCoordinatesVersionUsesBaseVersionOfArtifact() throws IOException {
		Artifact snapshotArtifact = mock(Artifact.class);
		given(snapshotArtifact.getScope()).willReturn("compile");
		given(snapshotArtifact.getArtifactId()).willReturn("artifact");
		given(snapshotArtifact.getBaseVersion()).willReturn("1.0-SNAPSHOT");
		given(snapshotArtifact.getFile()).willReturn(new File("a"));
		given(snapshotArtifact.getArtifactHandler()).willReturn(this.artifactHandler);
		this.artifacts = Collections.singleton(snapshotArtifact);
		new ArtifactsLibraries(this.artifacts, null, mock(Log.class)).doWithLibraries(
				(library) -> assertThat(library.getCoordinates().getVersion()).isEqualTo("1.0-SNAPSHOT"));
	}

}
