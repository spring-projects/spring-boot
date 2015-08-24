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

package org.springframework.boot.maven;

import java.util.List;

import org.apache.maven.artifact.Artifact;

/**
 * An {DependencyFilter} that filters out any artifact matching an {@link Exclude}.
 *
 * @author Stephane Nicoll
 * @author David Turanski
 * @since 1.1
 */
public class ExcludeFilter extends DependencyFilter {

	public ExcludeFilter(List<Exclude> excludes) {
		super(excludes);
	}

	@Override
	protected boolean filter(Artifact artifact) {
		for (FilterableDependency dependency : getFilters()) {
			if (equals(artifact, dependency)) {
				return true;
			}
		}
		return false;
	}

}
