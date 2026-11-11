/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.antora;

import org.gradle.api.Project;

/**
 * A contribution of catalog content.
 *
 * @author Andy Wilkinson
 */
class CatalogContentContribution extends ConsumableContentContribution {

	CatalogContentContribution(Project project, String name) {
		super(project, "catalog", name);
	}

}
