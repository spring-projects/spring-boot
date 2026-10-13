/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

interface SampleRepository extends MongoRepository<SampleDocument, String> {

}
