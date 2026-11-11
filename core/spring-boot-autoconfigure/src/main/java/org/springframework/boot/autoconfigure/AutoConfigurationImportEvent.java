/*
 * Copyright 2012-present the original author or authors.
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

	public AutoConfigurationImportEvent(Object source, List<String> candidateConfigurations, Set<String> exclusions) {
		super(source);
		this.candidateConfigurations = Collections.unmodifiableList(candidateConfigurations);
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
