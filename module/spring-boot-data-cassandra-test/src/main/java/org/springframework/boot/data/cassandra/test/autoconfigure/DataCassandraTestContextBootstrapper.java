/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.cassandra.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link DataCassandraTest @DataCassandraTest}
 * support.
 *
 * @author Artsiom Yudovin
 */
class DataCassandraTestContextBootstrapper extends TestSliceTestContextBootstrapper<DataCassandraTest> {

}
