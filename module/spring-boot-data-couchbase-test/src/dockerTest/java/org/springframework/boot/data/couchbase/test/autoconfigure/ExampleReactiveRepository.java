/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.couchbase.test.autoconfigure;

import org.springframework.data.couchbase.repository.ReactiveCouchbaseRepository;

/**
 * Example reactive repository used with {@link DataCouchbaseTest @DataCouchbaseTest}
 * tests.
 *
 * @author Eddú Meléndez
 */
interface ExampleReactiveRepository extends ReactiveCouchbaseRepository<ExampleDocument, String> {

}
