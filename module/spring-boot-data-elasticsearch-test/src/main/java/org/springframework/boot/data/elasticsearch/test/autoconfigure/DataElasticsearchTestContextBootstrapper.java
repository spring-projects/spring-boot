/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.elasticsearch.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for
 * {@link DataElasticsearchTest @DataElasticsearchTest} support.
 *
 * @author Eddú Meléndez
 */
class DataElasticsearchTestContextBootstrapper extends TestSliceTestContextBootstrapper<DataElasticsearchTest> {

}
