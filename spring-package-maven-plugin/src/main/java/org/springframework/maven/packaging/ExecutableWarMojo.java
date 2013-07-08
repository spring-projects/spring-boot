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

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Build an executable WAR file.
 * 
 * @author Phillip Webb
 */
@Mojo(name = "executable-war", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ExecutableWarMojo extends AbstractExecutableArchiveMojo {

	// FIXME classes in the wrong place

	private static final Map<String, String> SCOPE_DESTINATIONS;
	static {
		Map<String, String> map = new HashMap<String, String>();
		map.put("compile", "WEB-INF/lib/");
		map.put("runtime", "WEB-INF/lib/");
		map.put("provided", "WEB-INF/lib-provided/");
		SCOPE_DESTINATIONS = Collections.unmodifiableMap(map);
	}

	/**
	 * Single directory for extra files to include in the WAR. This is where you place
	 * your JSP files.
	 */
	@Parameter(defaultValue = "${basedir}/src/main/webapp", required = true)
	private File warSourceDirectory;

	@Override
	protected String getType() {
		return "executable-war";
	}

	@Override
	protected String getExtension() {
		return "war";
	}

	@Override
	protected String getArtifactDestination(Artifact artifact) {
		return SCOPE_DESTINATIONS.get(artifact.getScope());
	}

	@Override
	protected String getClassesDirectoryPrefix() {
		return "WEB-INF/classes/";
	}

	@Override
	protected void addContent(MavenArchiver archiver) {
		super.addContent(archiver);
		if (this.warSourceDirectory.exists()) {
			archiver.getArchiver().addDirectory(this.warSourceDirectory, getIncludes(),
					getExcludes());
		}
	}

	@Override
	protected String getLauncherClass() {
		return "org.springframework.launcher.WarLauncher";
	}
}
