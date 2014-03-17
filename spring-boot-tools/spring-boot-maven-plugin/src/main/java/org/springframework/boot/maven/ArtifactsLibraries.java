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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.springframework.boot.loader.tools.Libraries;
import org.springframework.boot.loader.tools.LibraryCallback;
import org.springframework.boot.loader.tools.LibraryScope;

/**
 * {@link Libraries} backed by Maven {@link Artifact}s
 * 
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class ArtifactsLibraries implements Libraries {

	private static final Map<String, LibraryScope> SCOPES;
	static {
		Map<String, LibraryScope> scopes = new HashMap<String, LibraryScope>();
		scopes.put(Artifact.SCOPE_COMPILE, LibraryScope.COMPILE);
		scopes.put(Artifact.SCOPE_RUNTIME, LibraryScope.RUNTIME);
		scopes.put(Artifact.SCOPE_PROVIDED, LibraryScope.PROVIDED);
		SCOPES = Collections.unmodifiableMap(scopes);
	}

	private final Set<Artifact> artifacts;

	public ArtifactsLibraries(Set<Artifact> artifacts) {
		this.artifacts = artifacts;
	}

	@Override
	public void doWithLibraries(LibraryCallback callback) throws IOException {
		for (Artifact artifact : this.artifacts) {
			LibraryScope scope = SCOPES.get(artifact.getScope());
			if (scope != null && artifact.getFile() != null) {
				callback.library(artifact.getFile(), scope);
			}
		}
	}
}
