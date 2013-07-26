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

package org.springframework.boot.maven;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.Artifact;

/**
 * Build an executable WAR file.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public class ExecutableWarHelper implements ArchiveHelper {

	private static final Map<String, String> SCOPE_DESTINATIONS;
	static {
		Map<String, String> map = new HashMap<String, String>();
		map.put("compile", "WEB-INF/lib/");
		map.put("runtime", "WEB-INF/lib/");
		map.put("provided", "WEB-INF/lib-provided/");
		SCOPE_DESTINATIONS = Collections.unmodifiableMap(map);
	}

	@Override
	public String getArtifactDestination(Artifact artifact) {
		return SCOPE_DESTINATIONS.get(artifact.getScope());
	}

	@Override
	public String getLauncherClass() {
		return "org.springframework.boot.load.WarLauncher";
	}
}
