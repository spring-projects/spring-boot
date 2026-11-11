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
 * Annotation that can be used to specify the name when binding to a property. This
 * annotation may be required when binding to names that clash with reserved language
 * keywords.
 * <p>
 * When naming a JavaBean-based property, annotate the field. When naming a
 * constructor-bound property, annotate the constructor parameter or record component.
 *
 * @author Phillip Webb
 * @author Lasse Wulff
 * @since 2.4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Documented
public @interface Name {

	/**
	 * The name of the property to use for binding.
	 * @return the property name
	 */
	String value();

}
