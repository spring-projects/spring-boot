/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.couchbase.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link DataCouchbaseTest @DataCouchbaseTest}
 * support.
 *
 * @author Eddú Meléndez
 */
class DataCouchbaseTestContextBootstrapper extends TestSliceTestContextBootstrapper<DataCouchbaseTest> {

}
