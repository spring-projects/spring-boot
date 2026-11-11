/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.springframework.test.context.MergedContextConfiguration;

/**
 * Encapsulates the <em>merged</em> context configuration declared on a test class and all
 * of its superclasses for a reactive web application.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class ReactiveWebMergedContextConfiguration extends MergedContextConfiguration {

	public ReactiveWebMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		super(mergedConfig);
	}

}
