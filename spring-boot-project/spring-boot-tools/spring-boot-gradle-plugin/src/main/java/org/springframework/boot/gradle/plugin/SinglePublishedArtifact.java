/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.gradle.plugin;

import org.gradle.api.Buildable;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.PublishArtifactSet;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

import org.springframework.boot.gradle.tasks.bundling.BootJar;
import org.springframework.boot.gradle.tasks.bundling.BootWar;

/**
 * A wrapper for a {@link PublishArtifactSet} that ensures that only a single artifact is
 * published, with a war file taking precedence over a jar file.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
final class SinglePublishedArtifact implements Buildable {

	private final Configuration configuration;

	private final ArtifactHandler handler;

	private PublishArtifact currentArtifact;

	SinglePublishedArtifact(Configuration configuration, ArtifactHandler handler) {
		this.configuration = configuration;
		this.handler = handler;
	}

	void addWarCandidate(TaskProvider<BootWar> candidate) {
		add(candidate);
	}

	void addJarCandidate(TaskProvider<BootJar> candidate) {
		if (this.currentArtifact == null) {
			add(candidate);
		}
	}

	private void add(TaskProvider<? extends Jar> artifact) {
		this.configuration.getArtifacts().remove(this.currentArtifact);
		this.currentArtifact = this.handler.add(this.configuration.getName(), artifact);
	}

	@Override
	public TaskDependency getBuildDependencies() {
		return this.configuration.getArtifacts().getBuildDependencies();
	}

}
