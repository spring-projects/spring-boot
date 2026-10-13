/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.test.autoconfigure;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * {@link TypeExcludeFilter} for {@link DataNeo4jTest @DataNeo4jTest}.
 *
 * @author Eddú Meléndez
 */
class DataNeo4jTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<DataNeo4jTest> {

	DataNeo4jTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

}
