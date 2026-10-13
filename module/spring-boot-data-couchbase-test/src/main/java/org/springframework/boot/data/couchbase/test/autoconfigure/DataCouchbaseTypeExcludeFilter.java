/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.couchbase.test.autoconfigure;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * {@link TypeExcludeFilter} for {@link DataCouchbaseTest @DataCouchbaseaTest}.
 *
 * @author Eddú Meléndez
 */
class DataCouchbaseTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<DataCouchbaseTest> {

	DataCouchbaseTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

}
