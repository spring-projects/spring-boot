/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jdbc.test.autoconfigure;

import org.springframework.data.repository.CrudRepository;

/**
 * Example repository used with {@link DataJdbcTest @DataJdbcTest} tests.
 *
 * @author Andy Wilkinson
 */
interface ExampleRepository extends CrudRepository<ExampleEntity, Long> {

}
