/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.build;

import org.antora.gradle.AntoraPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

/**
 * Plugin to apply conventions to projects that are part of Spring Boot's build.
 * Conventions are applied in response to various plugins being applied.
 *
 * When the {@link JavaBasePlugin} is applied, the conventions in {@link JavaConventions}
 * are applied.
 *
 * When the {@link MavenPublishPlugin} is applied, the conventions in
 * {@link MavenPublishingConventions} are applied.
 *
 * When the {@link AntoraPlugin} is applied, the conventions in {@link AntoraConventions}
 * are applied.
 *
 * @author Andy Wilkinson
 * @author Christoph Dreis
 * @author Mike Smithson
 */
public class ConventionsPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		new NoHttpConventions().apply(project);
		new JavaConventions().apply(project);
		new MavenPublishingConventions().apply(project);
		new AntoraConventions().apply(project);
		new KotlinConventions().apply(project);
		new WarConventions().apply(project);
		new EclipseConventions().apply(project);
		new TestFixturesConventions().apply(project);
		RepositoryTransformersExtension.apply(project);
	}

}
