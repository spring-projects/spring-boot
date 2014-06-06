/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional} that only matches when a property
 * has a given value.
 *
 * <p>If {@link #defaultMatch()} is {@code true} then the
 * condition <strong>also</strong> matches if the property
 * is not present at all.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
@Conditional(OnPropertyValueCondition.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ConditionalOnPropertyValue {

	/**
	 * A prefix that should be applied to the property.
	 * Defaults to no prefix. The prefix automatically
	 * ends with a dot, it does not need to be added.
	 */
	String prefix() default "";

	/**
	 * The property to check. If a prefix has been defined, it
	 * is applied to compute the full key of the property. For
	 * instance if the prefix is {@code app.config} and this
	 * property is {@code my-value}, the fully key would be
	 * {@code app.config.my-value}
	 * <p>Use the dashed notation to specify the property, that
	 * is all lower case with a "-" to separate words (e.g.
	 * {@code my-long-property})
	 */
	String property();

	/**
	 * If relaxed names should be checked. Defaults to {@code true}.
	 */
	boolean relaxedName() default true;

	/**
	 * The string representation of the expected value for the property.
	 */
	String value();

	/**
	 * Specify if the condition should match if the property is not set.
	 * Defaults to {@code false}
	 * <p>This means that the specified {@link #value()} is actually
	 * the default one (i.e. the one that you expect if the property
	 * is not set).
	 */
	boolean defaultMatch() default false;

}
