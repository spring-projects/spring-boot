/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.maven.packaging;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;

/**
 * Help build an executable JAR file.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public class ExecutableJarHelper implements ArchiveHelper {

	private static final Set<String> LIB_SCOPES = new HashSet<String>(Arrays.asList(
			"compile", "runtime", "provided"));

	@Override
	public String getArtifactDestination(Artifact artifact) {
		if (LIB_SCOPES.contains(artifact.getScope())) {
			return "lib/";
		}
		return null;
	}

	@Override
	public String getLauncherClass() {
		return "org.springframework.launcher.JarLauncher";
	}
}
