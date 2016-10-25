/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.loader.thin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.locator.DefaultModelLocator;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;

import org.springframework.core.io.Resource;

/**
 * Utility class to help with reading and extracting dependencies from a physical pom.
 *
 * @author Dave Syer
 *
 */
public class PomLoader {

	public List<Dependency> getDependencies(Resource pom) {
		if (!pom.exists()) {
			return Collections.emptyList();
		}
		Model model = readModel(pom);
		return convert(model.getDependencies());
	}

	public List<Dependency> getDependencyManagement(Resource pom) {
		if (!pom.exists()) {
			return Collections.emptyList();
		}
		List<Dependency> list = new ArrayList<Dependency>();
		Model model = readModel(pom);
		if (model.getParent() != null) {
			list.add(new Dependency(getParentArtifact(model), "import"));
		}
		if (model.getDependencyManagement() != null) {
			list.addAll(convert(model.getDependencyManagement().getDependencies()));
		}
		return list;
	}

	private Artifact getParentArtifact(Model model) {
		Parent parent = model.getParent();
		return new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(), "pom",
				parent.getVersion());
	}

	private List<Dependency> convert(
			List<org.apache.maven.model.Dependency> dependencies) {
		List<Dependency> result = new ArrayList<Dependency>();
		for (org.apache.maven.model.Dependency dependency : dependencies) {
			String scope = dependency.getScope();
			if (!"test".equals(scope) && !"provided".equals(scope)) {
				result.add(new Dependency(artifact(dependency), dependency.getScope()));
			}
		}
		return result;
	}

	private Artifact artifact(org.apache.maven.model.Dependency dependency) {
		return new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(),
				dependency.getClassifier(), dependency.getType(),
				dependency.getVersion());
	}

	private static Model readModel(Resource resource) {
		DefaultModelProcessor modelProcessor = new DefaultModelProcessor();
		modelProcessor.setModelLocator(new DefaultModelLocator());
		modelProcessor.setModelReader(new DefaultModelReader());

		try {
			return modelProcessor.read(resource.getInputStream(), null);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to build model from effective pom",
					ex);
		}
	}

}
