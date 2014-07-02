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
 * {@link Conditional} that only matches when the specified properties are defined in
 * {@link Environment} and not "false".
 *
 * @author Maciej Walkowiak
 * @since 1.1.0
 */
@Conditional(OnPropertyCondition.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ConditionalOnProperty {

	/**
	 * A prefix that should be applied to each property.
	 */
	String prefix() default "";

	/**
	 * One or more properties that must be present. If you are checking relaxed names you
	 * should specify the property in its dashed form.
	 * @return the property names
	 */
	String[] value();

	/**
	 * If relaxed names should be checked. Defaults to {@code true}.
	 */
	boolean relaxedNames() default true;

}
