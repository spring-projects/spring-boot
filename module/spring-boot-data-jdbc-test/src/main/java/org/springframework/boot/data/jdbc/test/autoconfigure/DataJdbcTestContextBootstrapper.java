/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jdbc.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link DataJdbcTest @DataJdbcTest} support.
 *
 * @author Andy Wilkinson
 */
class DataJdbcTestContextBootstrapper extends TestSliceTestContextBootstrapper<DataJdbcTest> {

}
