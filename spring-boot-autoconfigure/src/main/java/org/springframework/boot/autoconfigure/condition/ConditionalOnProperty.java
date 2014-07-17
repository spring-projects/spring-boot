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
import org.springframework.core.env.Environment;

/**
 * {@link Conditional} that checks if the specified properties
 * have the requested matching value. By default the properties
 * must be present in the {@link Environment} ant <strong>not</strong>
 * equal to {@code false}. The {@link #match()} and {@link #defaultMatch()}
 * attributes allow to further customize the condition.
 *
 * <p>The {@link #match} attribute provides the value that the property
 * should have. The {@link #defaultMatch()} flag specifies if the
 * condition <strong>also</strong> matches if the property is not present
 * at all.
 *
 * <p>The table below defines when a condition match according to the
 * property value and the {@link #match()} value
 *
 * <table border="1">
 * <th>
 *   <td>no {@code match} value</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code foo}</td>
 * </th>
 * <tr>
 *     <td>not set ({@code defaultMatch = false})</td>
 *     <td>no</td>
 *     <td>no</td>
 *     <td>no</td>
 *     <td>no</td>
 * </tr>
 * <tr>
 *     <td>not set ({@code defaultMatch = true})</td>
 *     <td>yes</td>
 *     <td>yes</td>
 *     <td>yes</td>
 *     <td>yes</td>
 * </tr>
 * <tr>
 *     <td>{@code true}</td>
 *     <td>yes</td>
 *     <td>yes</td>
 *     <td>no</td>
 *     <td>no</td>
 * </tr>
 * <tr>
 *     <td>{@code false}</td>
 *     <td>no</td>
 *     <td>no</td>
 *     <td>yes</td>
 *     <td>no</td>
 * </tr>
 * <tr>
 *     <td>{@code foo}</td>
 *     <td>yes</td>
 *     <td>no</td>
 *     <td>no</td>
 *     <td>yes</td>
 * </tr>
 * </table>
 *
 * @author Maciej Walkowiak
 * @author Stephane Nicoll
 * @since 1.1.0
 */
@Conditional(OnPropertyCondition.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ConditionalOnProperty {

	/**
	 * A prefix that should be applied to each property.
	 * <p>Defaults to no prefix. The prefix automatically
	 * ends with a dot if not specified.
	 */
	String prefix() default "";

	/**
	 * One or more properties to validate against the
	 * {@link #match} value. If a prefix has been defined, it
	 * is applied to compute the full key of each property. For
	 * instance if the prefix is {@code app.config} and one
	 * value is {@code my-value}, the fully key would be
	 * {@code app.config.my-value}
	 * <p>Use the dashed notation to specify each property, that
	 * is all lower case with a "-" to separate words (e.g.
	 * {@code my-long-property}).
	 * @return the property names
	 */
	String[] value();

	/**
	 * The string representation of the expected value for the
	 * properties. If not specified, the property must
	 * <strong>not</strong> be equals to {@code false}
	 */
	String match() default "";

	/**
	 * Specify if the condition should match if the property is not set.
	 * Defaults to {@code false}
	 */
	boolean defaultMatch() default false;

	/**
	 * If relaxed names should be checked. Defaults to {@code true}.
	 */
	boolean relaxedNames() default true;

}
