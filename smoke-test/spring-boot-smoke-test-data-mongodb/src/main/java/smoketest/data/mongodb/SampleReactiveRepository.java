/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.mongodb;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

interface SampleReactiveRepository extends ReactiveMongoRepository<SampleDocument, String> {

}
