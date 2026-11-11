/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.elasticsearch.test.autoconfigure;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * {@link TypeExcludeFilter} for {@link DataElasticsearchTest @DataElasticsearchTest}.
 *
 * @author Eddú Meléndez
 */
class DataElasticsearchTypeExcludeFilter
		extends StandardAnnotationCustomizableTypeExcludeFilter<DataElasticsearchTest> {

	DataElasticsearchTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

}
