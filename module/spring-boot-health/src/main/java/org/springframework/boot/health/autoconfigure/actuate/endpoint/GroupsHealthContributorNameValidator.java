/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.actuate.endpoint;

import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroups;
import org.springframework.boot.health.registry.HealthContributorNameValidator;
import org.springframework.util.Assert;

/**
 * {@link HealthContributorNameValidator} to ensure names don't clash with groups.
 *
 * @author Phillip Webb
 */
class GroupsHealthContributorNameValidator implements HealthContributorNameValidator {

	private final Set<String> groupNames;

	GroupsHealthContributorNameValidator(@Nullable HealthEndpointGroups groups) {
		this.groupNames = (groups != null) ? groups.getNames() : Collections.emptySet();
	}

	@Override
	public void validate(String name) throws IllegalStateException {
		Assert.state(!this.groupNames.contains(name),
				() -> "HealthContributor with name \"" + name + "\" clashes with group");
	}

}
