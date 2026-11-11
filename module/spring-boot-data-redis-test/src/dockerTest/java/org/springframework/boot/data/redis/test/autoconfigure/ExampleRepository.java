/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.test.autoconfigure;

import org.springframework.data.repository.CrudRepository;

/**
 * Example repository used with {@link DataRedisTest @DataRedisTest} tests.
 *
 * @author Jayaram Pradhan
 */
interface ExampleRepository extends CrudRepository<PersonHash, String> {

}
