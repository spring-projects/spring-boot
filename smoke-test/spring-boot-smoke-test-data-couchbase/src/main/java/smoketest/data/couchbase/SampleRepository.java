/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.couchbase;

import org.springframework.data.couchbase.repository.CouchbaseRepository;

interface SampleRepository extends CouchbaseRepository<SampleDocument, String> {

}
