/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.couchbase.repositories

import org.springframework.core.convert.converter.Converter

internal class MyConverter : Converter<CouchbaseProperties, Boolean> {

	override fun convert(value: CouchbaseProperties): Boolean {
		return true
	}

}

