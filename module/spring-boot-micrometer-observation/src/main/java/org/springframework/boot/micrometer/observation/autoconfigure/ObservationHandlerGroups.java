/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.observation.autoconfigure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry.ObservationConfig;
import org.jspecify.annotations.Nullable;

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
		List<ObservationHandler<?>> unclaimed = new ArrayList<>();
		for (ObservationHandler<?> handler : handlers) {
			ObservationHandlerGroup group = findGroup(handler);
			if (group == null) {
				unclaimed.add(handler);
			}
			else {
				grouped.add(group, handler);
			}
		}
		for (ObservationHandlerGroup group : this.groups) {
			List<ObservationHandler<?>> members = grouped.get(group);
			if (!CollectionUtils.isEmpty(members)) {
				group.registerMembers(config, members);
			}
		}
		if (!CollectionUtils.isEmpty(unclaimed)) {
			for (ObservationHandler<?> handler : unclaimed) {
				config.observationHandler(handler);
			}
		}
	}

	private @Nullable ObservationHandlerGroup findGroup(ObservationHandler<?> handler) {
		for (ObservationHandlerGroup group : this.groups) {
			if (group.isMember(handler)) {
				return group;
			}
		}
		return null;
	}

}
