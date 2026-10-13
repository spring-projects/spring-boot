/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.boot.test.context.ReactiveWebMergedContextConfiguration;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link WebFluxTest @WebFluxTest} support.
 *
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 */
class WebFluxTestContextBootstrapper extends TestSliceTestContextBootstrapper<WebFluxTest> {

	@Override
	protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		return new ReactiveWebMergedContextConfiguration(super.processMergedContextConfiguration(mergedConfig));
	}

}
