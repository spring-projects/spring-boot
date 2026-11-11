/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.cassandra.test.autoconfigure;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * {@link TypeExcludeFilter} for {@link DataCassandraTest @DataCassandraTest}.
 *
 * @author Artsiom Yudovin
 */
class DataCassandraTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<DataCassandraTest> {

	DataCassandraTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

}
