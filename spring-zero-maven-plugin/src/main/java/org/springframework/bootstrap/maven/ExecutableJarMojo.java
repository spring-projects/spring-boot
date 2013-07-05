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

package org.springframework.bootstrap.maven;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Build an executable JAR file.
 * 
 * @author Phillip Webb
 */
@Mojo(name = "executable-jar", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ExecutableJarMojo extends AbstractExecutableArchiveMojo {

	private static final Set<String> LIB_SCOPES = new HashSet<String>(Arrays.asList(
			"compile", "runtime", "provided"));

	@Override
	protected String getType() {
		return "executable-jar";
	}

	@Override
	protected String getExtension() {
		return "jar";
	}

	@Override
	protected String getArtifactDestination(Artifact artifact) {
		if (LIB_SCOPES.contains(artifact.getScope())) {
			return "lib/";
		}
		return null;
	}

	@Override
	protected String getLauncherClass() {
		return "org.springframework.bootstrap.launcher.JarLauncher";
	}
}
