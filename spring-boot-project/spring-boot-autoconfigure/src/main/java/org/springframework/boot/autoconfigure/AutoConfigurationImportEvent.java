/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.Set;

/**
 * Event fired when auto-configuration classes are imported.
 *
 * @author Phillip Webb
 * @since 1.5.0
 */
public class AutoConfigurationImportEvent extends EventObject {

	private final List<String> candidateConfigurations;

	private final Set<String> exclusions;

	public AutoConfigurationImportEvent(Object source,
			List<String> candidateConfigurations, Set<String> exclusions) {
		super(source);
		this.candidateConfigurations = Collections
				.unmodifiableList(candidateConfigurations);
		this.exclusions = Collections.unmodifiableSet(exclusions);
	}

	/**
	 * Return the auto-configuration candidate configurations that are going to be
	 * imported.
	 * @return the auto-configuration candidates
	 */
	public List<String> getCandidateConfigurations() {
		return this.candidateConfigurations;
	}

	/**
	 * Return the exclusions that were applied.
	 * @return the exclusions applied
	 */
	public Set<String> getExclusions() {
		return this.exclusions;
	}

}
