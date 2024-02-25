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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import org.springframework.boot.loader.tools.Libraries;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCallback;
import org.springframework.boot.loader.tools.LibraryCoordinates;
import org.springframework.boot.loader.tools.LibraryScope;

/**
 * {@link Libraries} backed by Maven {@link Artifact}s.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 1.0.0
 */
public class ArtifactsLibraries implements Libraries {

	private static final Map<String, LibraryScope> SCOPES;

	static {
		Map<String, LibraryScope> libraryScopes = new HashMap<>();
		libraryScopes.put(Artifact.SCOPE_COMPILE, LibraryScope.COMPILE);
		libraryScopes.put(Artifact.SCOPE_RUNTIME, LibraryScope.RUNTIME);
		libraryScopes.put(Artifact.SCOPE_PROVIDED, LibraryScope.PROVIDED);
		libraryScopes.put(Artifact.SCOPE_SYSTEM, LibraryScope.PROVIDED);
		SCOPES = Collections.unmodifiableMap(libraryScopes);
	}

	private final Set<Artifact> artifacts;

	private final Set<Artifact> includedArtifacts;

	private final Collection<MavenProject> localProjects;

	private final Collection<Dependency> unpacks;

	private final Log log;

	/**
	 * Creates a new {@code ArtifactsLibraries} from the given {@code artifacts}.
	 * @param artifacts the artifacts to represent as libraries
	 * @param localProjects projects for which {@link Library#isLocal() local} libraries
	 * should be created
	 * @param unpacks artifacts that should be unpacked on launch
	 * @param log the log
	 * @since 2.4.0
	 */
	public ArtifactsLibraries(Set<Artifact> artifacts, Collection<MavenProject> localProjects,
			Collection<Dependency> unpacks, Log log) {
		this(artifacts, artifacts, localProjects, unpacks, log);
	}

	/**
	 * Creates a new {@code ArtifactsLibraries} from the given {@code artifacts}.
	 * @param artifacts all artifacts that can be represented as libraries
	 * @param includedArtifacts the actual artifacts to include in the uber jar
	 * @param localProjects projects for which {@link Library#isLocal() local} libraries
	 * should be created
	 * @param unpacks artifacts that should be unpacked on launch
	 * @param log the log
	 * @since 2.4.8
	 */
	public ArtifactsLibraries(Set<Artifact> artifacts, Set<Artifact> includedArtifacts,
			Collection<MavenProject> localProjects, Collection<Dependency> unpacks, Log log) {
		this.artifacts = artifacts;
		this.includedArtifacts = includedArtifacts;
		this.localProjects = localProjects;
		this.unpacks = unpacks;
		this.log = log;
	}

	/**
     * Iterates over the list of artifacts and performs the specified callback operation on each library.
     * 
     * @param callback the callback function to be executed on each library
     * @throws IOException if an I/O error occurs
     */
    @Override
	public void doWithLibraries(LibraryCallback callback) throws IOException {
		Set<String> duplicates = getDuplicates(this.artifacts);
		for (Artifact artifact : this.artifacts) {
			String name = getFileName(artifact);
			File file = artifact.getFile();
			LibraryScope scope = SCOPES.get(artifact.getScope());
			if (scope == null || file == null) {
				continue;
			}
			if (duplicates.contains(name)) {
				this.log.debug("Duplicate found: " + name);
				name = artifact.getGroupId() + "-" + name;
				this.log.debug("Renamed to: " + name);
			}
			LibraryCoordinates coordinates = new ArtifactLibraryCoordinates(artifact);
			boolean unpackRequired = isUnpackRequired(artifact);
			boolean local = isLocal(artifact);
			boolean included = this.includedArtifacts.contains(artifact);
			callback.library(new Library(name, file, scope, coordinates, unpackRequired, local, included));
		}
	}

	/**
     * Returns a set of duplicate file names from the given set of artifacts.
     * 
     * @param artifacts the set of artifacts to check for duplicates
     * @return a set of duplicate file names
     */
    private Set<String> getDuplicates(Set<Artifact> artifacts) {
		Set<String> duplicates = new HashSet<>();
		Set<String> seen = new HashSet<>();
		for (Artifact artifact : artifacts) {
			String fileName = getFileName(artifact);
			if (artifact.getFile() != null && !seen.add(fileName)) {
				duplicates.add(fileName);
			}
		}
		return duplicates;
	}

	/**
     * Checks if unpacking is required for a given artifact.
     * 
     * @param artifact the artifact to check
     * @return true if unpacking is required, false otherwise
     */
    private boolean isUnpackRequired(Artifact artifact) {
		if (this.unpacks != null) {
			for (Dependency unpack : this.unpacks) {
				if (artifact.getGroupId().equals(unpack.getGroupId())
						&& artifact.getArtifactId().equals(unpack.getArtifactId())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
     * Checks if the given artifact is present in the local projects or their attached artifacts.
     * 
     * @param artifact The artifact to be checked
     * @return true if the artifact is found in the local projects or their attached artifacts, false otherwise
     */
    private boolean isLocal(Artifact artifact) {
		for (MavenProject localProject : this.localProjects) {
			if (localProject.getArtifact().equals(artifact)) {
				return true;
			}
			for (Artifact attachedArtifact : localProject.getAttachedArtifacts()) {
				if (attachedArtifact.equals(artifact)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
     * Returns the file name for the given artifact.
     * The file name is generated by concatenating the artifact's artifactId, baseVersion, and classifier (if present),
     * followed by the extension of the artifact's artifactHandler.
     *
     * @param artifact the artifact for which to generate the file name
     * @return the file name for the given artifact
     */
    private String getFileName(Artifact artifact) {
		StringBuilder sb = new StringBuilder();
		sb.append(artifact.getArtifactId()).append("-").append(artifact.getBaseVersion());
		String classifier = artifact.getClassifier();
		if (classifier != null) {
			sb.append("-").append(classifier);
		}
		sb.append(".").append(artifact.getArtifactHandler().getExtension());
		return sb.toString();
	}

	/**
	 * {@link LibraryCoordinates} backed by a Maven {@link Artifact}.
	 */
	private static class ArtifactLibraryCoordinates implements LibraryCoordinates {

		private final Artifact artifact;

		/**
         * Sets the artifact for the ArtifactLibraryCoordinates.
         * 
         * @param artifact the artifact to be set
         */
        ArtifactLibraryCoordinates(Artifact artifact) {
			this.artifact = artifact;
		}

		/**
         * Returns the group ID of the artifact.
         *
         * @return the group ID of the artifact
         */
        @Override
		public String getGroupId() {
			return this.artifact.getGroupId();
		}

		/**
         * Returns the artifact ID of the ArtifactLibraryCoordinates object.
         *
         * @return the artifact ID
         */
        @Override
		public String getArtifactId() {
			return this.artifact.getArtifactId();
		}

		/**
         * Returns the version of the artifact.
         * 
         * @return the version of the artifact
         */
        @Override
		public String getVersion() {
			return this.artifact.getBaseVersion();
		}

		/**
         * Returns a string representation of the object.
         * 
         * @return a string representation of the object
         */
        @Override
		public String toString() {
			return this.artifact.toString();
		}

	}

}
