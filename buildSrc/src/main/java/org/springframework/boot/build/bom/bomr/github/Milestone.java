/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom.bomr.github;

import java.time.OffsetDateTime;

/**
 * A milestone in a {@link GitHubRepository GitHub repository}.
 *
 * @author Andy Wilkinson
 */
public class Milestone {

	private final String name;

	private final int number;

	private final OffsetDateTime dueOn;

	Milestone(String name, int number, OffsetDateTime dueOn) {
		this.name = name;
		this.number = number;
		this.dueOn = dueOn;
	}

	/**
	 * Returns the name of the milestone.
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the number of the milestone.
	 * @return the number
	 */
	public int getNumber() {
		return this.number;
	}

	public OffsetDateTime getDueOn() {
		return this.dueOn;
	}

	@Override
	public String toString() {
		return this.name + " (" + this.number + ")";
	}

}
