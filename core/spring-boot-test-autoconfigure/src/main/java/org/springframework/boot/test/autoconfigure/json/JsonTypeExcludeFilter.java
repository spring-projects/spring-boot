/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure.json;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * {@link TypeExcludeFilter} for {@link JsonTest @JsonTest}.
 *
 * @author Phillip Webb
 */
class JsonTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<JsonTest> {

	JsonTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

}
