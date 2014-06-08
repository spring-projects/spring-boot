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

package org.springframework.boot.dependency.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A single node in a {@link DependencyTree}.
 * 
 * @author Phillip Webb
 * @see DependencyTree
 * @since 1.1.0
 */
class DependencyNode implements Iterable<DependencyNode> {

	private final String groupId;

	private final String artifactId;

	private final String version;

	private List<DependencyNode> dependencies;

	DependencyNode(String groupId, String artifactId, String version) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.dependencies = new ArrayList<DependencyNode>();
	}

	@Override
	public Iterator<DependencyNode> iterator() {
		return getDependencies().iterator();
	}

	public String getGroupId() {
		return this.groupId;
	}

	public String getArtifactId() {
		return this.artifactId;
	}

	public String getVersion() {
		return this.version;
	}

	public List<DependencyNode> getDependencies() {
		return Collections.unmodifiableList(this.dependencies);
	}

	@Override
	public String toString() {
		return this.groupId + ":" + this.artifactId + ":" + this.version;
	}

	void addDependency(DependencyNode node) {
		this.dependencies.add(node);
	}

	DependencyNode getLastDependency() {
		return this.dependencies.get(this.dependencies.size() - 1);
	}

}
