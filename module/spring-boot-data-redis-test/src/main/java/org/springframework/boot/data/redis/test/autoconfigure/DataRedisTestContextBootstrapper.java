/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.TestSliceTestContextBootstrapper;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link DataRedisTest @DataRedisTest} support.
 *
 * @author Artsiom Yudovin
 */
class DataRedisTestContextBootstrapper extends TestSliceTestContextBootstrapper<DataRedisTest> {

}
