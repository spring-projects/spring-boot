/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.couchbase.repositories;

import org.assertj.core.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.BeanNames;
import org.springframework.data.couchbase.core.convert.CouchbaseCustomConversions;

@Configuration(proxyBeanMethods = false)
public class MyCouchbaseConfiguration {

	@Bean(BeanNames.COUCHBASE_CUSTOM_CONVERSIONS)
	public CouchbaseCustomConversions myCustomConversions() {
		return new CouchbaseCustomConversions(Arrays.asList(new MyConverter()));
	}

}
