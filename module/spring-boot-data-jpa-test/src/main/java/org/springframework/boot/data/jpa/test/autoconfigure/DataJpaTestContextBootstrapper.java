/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jpa.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link DataJpaTest @DataJpaTest} support.
 *
 * @author Artsiom Yudovin
 */
class DataJpaTestContextBootstrapper extends TestSliceTestContextBootstrapper<DataJpaTest> {

}
