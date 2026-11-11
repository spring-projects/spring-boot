/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.bind;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation that should be added to annotations that indicate a field is a nested
 * type. Used to ensure that correct reflection hints are registered.
 *
 * @author Phillip Webb
 * @since 3.0.0
 * @see BindableRuntimeHintsRegistrar
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Documented
public @interface Nested {

}
