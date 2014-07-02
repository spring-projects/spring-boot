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

package org.springframework.boot.gradle.run;

import java.util.Collections;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

/**
 * Utilities for working with {@link SourceSet}s.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
class SourceSets {

	public static SourceSet findMainSourceSet(Project project) {
		for (SourceSet sourceSet : getJavaSourceSets(project)) {
			if (SourceSet.MAIN_SOURCE_SET_NAME.equals(sourceSet.getName())) {
				return sourceSet;
			}
		}
		return null;
	}

	private static Iterable<SourceSet> getJavaSourceSets(Project project) {
		JavaPluginConvention plugin = project.getConvention().getPlugin(
				JavaPluginConvention.class);
		if (plugin == null) {
			return Collections.emptyList();
		}
		return plugin.getSourceSets();
	}

}
