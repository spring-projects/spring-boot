/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.r2dbc.test.autoconfigure;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * Example {@link ReactiveCrudRepository} used with {@link DataR2dbcTest} tests.
 *
 * @author Mark Paluch
 */
public interface ExampleRepository extends ReactiveCrudRepository<Example, String> {

}
