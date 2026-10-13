/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.test.autoconfigure;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/**
 * Example reactive repository used with {@link DataMongoTest @DataMongoTest} tests.
 *
 * @author Stephane Nicoll
 */
interface ExampleReactiveRepository extends ReactiveMongoRepository<ExampleDocument, String> {

}
