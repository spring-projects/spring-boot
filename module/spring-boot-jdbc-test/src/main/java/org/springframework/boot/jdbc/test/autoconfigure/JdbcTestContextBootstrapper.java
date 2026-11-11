/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link JdbcTest @JdbcTest} support.
 *
 * @author Artsiom Yudovin
 * @author Yanming Zhou
 */
class JdbcTestContextBootstrapper extends TestSliceTestContextBootstrapper<JdbcTest> {

}
