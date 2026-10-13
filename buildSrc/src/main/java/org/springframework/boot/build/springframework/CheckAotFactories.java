/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.springframework;

import org.gradle.api.Task;

/**
 * {@link Task} that checks {@code META-INF/spring/aot.factories}.
 *
 * @author Andy Wilkinson
 */
public abstract class CheckAotFactories extends CheckFactoriesFile {

	public CheckAotFactories() {
		super("META-INF/spring/aot.factories");
	}

}
