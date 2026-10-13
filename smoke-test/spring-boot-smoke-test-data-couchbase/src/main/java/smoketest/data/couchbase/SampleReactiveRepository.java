/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.couchbase;

import org.springframework.data.couchbase.repository.ReactiveCouchbaseRepository;

interface SampleReactiveRepository extends ReactiveCouchbaseRepository<SampleDocument, String> {

}
