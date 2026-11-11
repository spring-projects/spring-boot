/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Example {@link PropertyMapping @PropertyMapping} annotation for use with
 * {@link PropertyMappingTests}.
 *
 * @author Phillip Webb
 */
@Retention(RetentionPolicy.RUNTIME)
@PropertyMapping
@interface ExampleMapping {

	String exampleProperty();

}
