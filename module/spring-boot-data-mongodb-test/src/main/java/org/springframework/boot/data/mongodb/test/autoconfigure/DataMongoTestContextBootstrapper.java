/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link DataMongoTest @DataMongoTest} support.
 *
 * @author Artsiom Yudovin
 */
class DataMongoTestContextBootstrapper extends TestSliceTestContextBootstrapper<DataMongoTest> {

}
