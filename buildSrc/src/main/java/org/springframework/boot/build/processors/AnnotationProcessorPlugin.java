/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.boot.build.processors;

import java.util.Map;
import java.util.TreeMap;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.bundling.Jar;

/**
 * A {@link Plugin} for an annotation processor project.
 *
 * @author Christoph Dreis
 */
public class AnnotationProcessorPlugin implements Plugin<Project> {

	private static final String JAR_TYPE = "annotation-processor";

	@Override
	public void apply(Project project) {
		project.getTasks().withType(Jar.class, (jar) -> project.afterEvaluate((evaluated) -> {
			jar.manifest((manifest) -> {
				Map<String, Object> attributes = new TreeMap<>();
				attributes.put("Spring-Boot-Jar-Type", JAR_TYPE);
				manifest.attributes(attributes);
			});
		}));
	}

}
