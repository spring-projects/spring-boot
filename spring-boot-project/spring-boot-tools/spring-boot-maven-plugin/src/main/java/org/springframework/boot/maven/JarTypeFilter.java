/*
 * Copyright 2012-2021 the original author or authors.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;

/**
 * A {@link DependencyFilter} that filters dependencies based on the jar type declared in
 * their manifest.
 *
 * @author Andy Wilkinson
 */
class JarTypeFilter extends DependencyFilter {

	private static final Set<String> EXCLUDED_JAR_TYPES = Collections
			.unmodifiableSet(new HashSet<>(Arrays.asList("annotation-processor", "dependencies-starter")));

	JarTypeFilter() {
		super(Collections.emptyList());
	}

	@Override
	protected boolean filter(Artifact artifact) {
		try (JarFile jarFile = new JarFile(artifact.getFile())) {
			Manifest manifest = jarFile.getManifest();
			if (manifest != null) {
				String jarType = manifest.getMainAttributes().getValue("Spring-Boot-Jar-Type");
				if (jarType != null && EXCLUDED_JAR_TYPES.contains(jarType)) {
					return true;
				}
			}
		}
		catch (IOException ex) {
			// Continue
		}
		return false;
	}

}
