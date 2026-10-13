/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.test.autoconfigure;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Example repository used with {@link DataMongoTest @DataMongoTest} tests.
 *
 * @author Michael Simons
 */
interface ExampleRepository extends MongoRepository<ExampleDocument, String> {

}
