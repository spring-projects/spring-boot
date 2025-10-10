/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.context.filter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.ApplicationContext;

/**
 * Annotation that can be on tests to define a set of {@link TypeExcludeFilter} classes
 * that should be registered with the {@link ApplicationContext}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see TypeExcludeFilter
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TypeExcludeFilters {

	/**
	 * Specifies {@link TypeExcludeFilter} classes that should be registered. Classes
	 * specified here can either have a no-arg constructor or accept a single
	 * {@code Class<?>} argument if they need access to the {@code testClass}.
	 * @see TypeExcludeFilter
	 * @return the type exclude filters to apply
	 */
	Class<? extends TypeExcludeFilter>[] value();

}
