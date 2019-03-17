/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.gradle.plugin;

import org.gradle.api.Buildable;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.PublishArtifactSet;
import org.gradle.api.tasks.TaskDependency;

/**
 * A wrapper for a {@link PublishArtifactSet} that ensures that only a single artifact is
 * published, with a war file taking precedence over a jar file.
 *
 * @author Andy Wilkinson
 */
final class SinglePublishedArtifact implements Buildable {

	private final PublishArtifactSet artifacts;

	private PublishArtifact currentArtifact;

	SinglePublishedArtifact(PublishArtifactSet artifacts) {
		this.artifacts = artifacts;
	}

	void addCandidate(PublishArtifact candidate) {
		if (this.currentArtifact == null || "war".equals(candidate.getExtension())) {
			this.artifacts.remove(this.currentArtifact);
			this.artifacts.add(candidate);
			this.currentArtifact = candidate;
		}
	}

	@Override
	public TaskDependency getBuildDependencies() {
		return this.currentArtifact.getBuildDependencies();
	}

}
