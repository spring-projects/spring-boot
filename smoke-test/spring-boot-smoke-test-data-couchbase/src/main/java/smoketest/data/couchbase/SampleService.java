/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.couchbase;

import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.stereotype.Service;

@Service
public class SampleService {

	private final CouchbaseTemplate couchbaseTemplate;

	public SampleService(CouchbaseTemplate couchbaseTemplate) {
		this.couchbaseTemplate = couchbaseTemplate;
	}

	public SampleDocument findById(String id) {
		return this.couchbaseTemplate.findById(SampleDocument.class).one(id);
	}

}
