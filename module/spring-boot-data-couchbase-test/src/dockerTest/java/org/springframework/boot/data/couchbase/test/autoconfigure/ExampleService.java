/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.couchbase.test.autoconfigure;

import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.stereotype.Service;

/**
 * Example service used with {@link DataCouchbaseTest @DataCouchbaseTest} tests.
 *
 * @author Eddú Meléndez
 */
@Service
public class ExampleService {

	private final CouchbaseTemplate couchbaseTemplate;

	public ExampleService(CouchbaseTemplate couchbaseTemplate) {
		this.couchbaseTemplate = couchbaseTemplate;
	}

	public ExampleDocument findById(String id) {
		return this.couchbaseTemplate.findById(ExampleDocument.class).one(id);
	}

}
