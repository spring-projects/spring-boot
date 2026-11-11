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
 * Annotation that can be used to specify the default value when binding to an immutable
 * property. This annotation can also be used with nested properties to indicate that a
 * value should always be bound (rather than binding {@code null}). The value from this
 * annotation will only be used if the property is not found in the property sources used
 * by the {@link Binder}. For example, if the property is present in the
 * {@link org.springframework.core.env.Environment} when binding to
 * {@link org.springframework.boot.context.properties.ConfigurationProperties @ConfigurationProperties},
 * the default value for the property will not be used even if the property value is
 * empty.
 * <p>
 * Property placeholders in the default value are resolved using the {@link Binder}'s
 * {@link PlaceholdersResolver} before the value is converted to the property's type.
 *
 * @author Madhura Bhave
 * @author Pavel Anisimov
 * @author Wan bin yu
 * @since 2.2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
@Documented
public @interface DefaultValue {

	/**
	 * The default value of the property. Can be an array of values for collection or
	 * array-based properties.
	 * @return the default value of the property.
	 */
	String[] value() default {};

}
