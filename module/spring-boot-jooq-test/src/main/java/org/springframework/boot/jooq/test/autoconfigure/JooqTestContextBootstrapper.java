/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link JooqTest @JooqTest} support.
 *
 * @author Artsiom Yudovin
 */
class JooqTestContextBootstrapper extends TestSliceTestContextBootstrapper<JooqTest> {

}
