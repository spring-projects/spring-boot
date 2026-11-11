/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.antora;

import org.gradle.api.Project;

/**
 * A contribution of aggregate content.
 *
 * @author Andy Wilkinson
 */
class AggregateContentContribution extends ConsumableContentContribution {

	protected AggregateContentContribution(Project project, String name) {
		super(project, "aggregate", name);
	}

}
