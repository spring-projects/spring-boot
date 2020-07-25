/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the Annotation class(es) which need to be checked on the main boot class
 * annotated with @SpringBootApplication. @ConditionalOnAnnotation annotation should work
 * on any @Configuration class, but the order of loading is not guaranteed. So annotating
 * at @SpringBootApplication main class makes sure this bean is loaded by the time the
 * condition is evaluated.
 *
 * Usage Examples: 1. @ConditionalOnAnnotation(EnableAsync.class)
 * 2. @ConditionalOnAnnotation({ EnableAsync.class, EnableCaching.class })
 * 3. @ConditionalOnAnnotation( value = { EnableAsync.class, EnableCaching.class },
 * conditionType = ConditionalOnAnnotation.ConditionType.AND )
 *
 * This Annotation will be useful when making a AutoConfiguration class as conditional.
 *
 * When placed on a {@code @Configuration} class, the configuration takes place only when
 * the specified annotation is present on main spring boot class.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ConditionalOnAnnotation(EnableAsync.class)
 * public class MyAutoConfiguration {
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         ...
 *     }
 * }</pre>
 *
 *
 * When placed on a {@code @Bean} method, the bean is created only when the specified
 * annotation is present on main spring boot class.
 *
 * <pre class="code">
 * &#064;Configuration
 * public class MyAppConfiguration {
 *
 *     &#064;ConditionalOnAnnotation(EnableAsync.class)
 *     &#064;Bean
 *     public MyService myService() {
 *         ...
 *     }
 * }</pre>
 *
 * @author Vimalraj Chandra Sekaran (Github: rajvimalc)
 * @since 2.4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnAnnotationCondition.class)
public @interface ConditionalOnAnnotation {

	/**
	 * Minimum 1 Annotation type class should be provided.
	 * @return the classes that must be present
	 */
	Class<? extends Annotation>[] value();

	/**
	 * When ConditionType is OR, the condition is true when any one of the annotations
	 * mentioned is present. When ConditionType is AND, the condition is true when all of
	 * the annotations mentioned are present.
	 * @return the ConditionType to be evaluated
	 */
	ConditionType conditionType() default ConditionType.OR;

	enum ConditionType {

		OR, AND

	}

}
