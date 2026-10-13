/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.springframework;

import org.gradle.api.Task;

/**
 * {@link Task} that checks {@code META-INF/spring.factories}.
 *
 * @author Andy Wilkinson
 */
public abstract class CheckSpringFactories extends CheckFactoriesFile {

	public CheckSpringFactories() {
		super("META-INF/spring.factories");
	}

}
