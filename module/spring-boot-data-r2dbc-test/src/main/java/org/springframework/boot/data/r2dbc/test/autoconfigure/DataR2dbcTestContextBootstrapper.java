/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.r2dbc.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link DataR2dbcTest @DataR2dbcTest} support.
 *
 * @author Mark Paluch
 */
class DataR2dbcTestContextBootstrapper extends TestSliceTestContextBootstrapper<DataR2dbcTest> {

}
