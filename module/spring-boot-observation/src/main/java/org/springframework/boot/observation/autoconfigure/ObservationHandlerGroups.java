/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.observation.autoconfigure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry.ObservationConfig;

import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * A collection of {@link ObservationHandlerGroup} instances and supporting registration
 * logic.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class ObservationHandlerGroups {

	private final List<ObservationHandlerGroup> groups;

	ObservationHandlerGroups(Collection<? extends ObservationHandlerGroup> groups) {
		this.groups = Collections.unmodifiableList(sort(groups));
	}

	private static List<ObservationHandlerGroup> sort(Collection<? extends ObservationHandlerGroup> groups) {
		ArrayList<ObservationHandlerGroup> sortedGroups = new ArrayList<>(groups);
		Collections.sort(sortedGroups);
		return sortedGroups;
	}

	void register(ObservationConfig config, List<ObservationHandler<?>> handlers) {
		MultiValueMap<ObservationHandlerGroup, ObservationHandler<?>> grouped = new LinkedMultiValueMap<>();
		for (ObservationHandler<?> handler : handlers) {
			grouped.add(findGroup(handler), handler);
		}
		for (ObservationHandlerGroup group : this.groups) {
			List<ObservationHandler<?>> members = grouped.get(group);
			if (!CollectionUtils.isEmpty(members)) {
				group.registerMembers(config, members);
			}
		}
		List<ObservationHandler<?>> unclaimed = grouped.get(null);
		if (!CollectionUtils.isEmpty(unclaimed)) {
			for (ObservationHandler<?> handler : unclaimed) {
				config.observationHandler(handler);
			}
		}
	}

	private ObservationHandlerGroup findGroup(ObservationHandler<?> handler) {
		for (ObservationHandlerGroup group : this.groups) {
			if (group.isMember(handler)) {
				return group;
			}
		}
		return null;
	}

}
