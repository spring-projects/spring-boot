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
 * Annotation that can be used to indicate which constructor to use when binding
 * configuration properties using constructor arguments rather than by calling setters. A
 * single parameterized constructor implicitly indicates that constructor binding should
 * be used unless the constructor is annotated with {@code @Autowired}.
 *
 * @author Phillip Webb
 * @since 3.0.0
 */
@Target({ ElementType.CONSTRUCTOR, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConstructorBinding {

}
