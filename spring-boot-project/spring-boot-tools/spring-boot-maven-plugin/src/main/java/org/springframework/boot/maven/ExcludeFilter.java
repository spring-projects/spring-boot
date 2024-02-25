/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.maven;

import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.Artifact;

/**
 * An {DependencyFilter} that filters out any artifact matching an {@link Exclude}.
 *
 * @author Stephane Nicoll
 * @author David Turanski
 * @since 1.1.0
 */
public class ExcludeFilter extends DependencyFilter {

	/**
     * Constructs a new ExcludeFilter with the specified Exclude objects.
     * 
     * @param excludes the Exclude objects to be used for filtering
     */
    public ExcludeFilter(Exclude... excludes) {
		this(Arrays.asList(excludes));
	}

	/**
     * Constructs a new ExcludeFilter with the specified list of excludes.
     * 
     * @param excludes the list of excludes to be used by the filter
     */
    public ExcludeFilter(List<Exclude> excludes) {
		super(excludes);
	}

	/**
     * Filters the given artifact based on the filters defined in the ExcludeFilter class.
     * 
     * @param artifact The artifact to be filtered.
     * @return {@code true} if the artifact matches any of the defined filters, {@code false} otherwise.
     */
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
