/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.couchbase.repositories;

import org.springframework.core.convert.converter.Converter;

class MyConverter implements Converter<CouchbaseProperties, Boolean> {

	@Override
	public Boolean convert(CouchbaseProperties value) {
		return true;
	}

}
