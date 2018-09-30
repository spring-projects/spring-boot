/*
 * Copyright 2012-2017 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional} that only matches when the specified bean classes and/or names are
 * not already contained in the {@link BeanFactory}.
 * <p>
 * When placed on a {@code @Bean} method, the bean class defaults to the return type of
 * the factory method:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class MyAutoConfiguration {
 *
 *     &#064;ConditionalOnMissingBean
 *     &#064;Bean
 *     public MyService myService() {
 *         ...
 *     }
 *
 * }</pre>
 * <p>
 * In the sample above the condition will match if no bean of type {@code MyService} is
 * already contained in the {@link BeanFactory}.
 * <p>
 * The condition can only match the bean definitions that have been processed by the
 * application context so far and, as such, it is strongly recommended to use this
 * condition on auto-configuration classes only. If a candidate bean may be created by
 * another auto-configuration, make sure that the one using this condition runs after.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnBeanCondition.class)
public @interface ConditionalOnMissingBean {

	/**
	 * The class type of bean that should be checked. The condition matches when each
	 * class specified is missing in the {@link ApplicationContext}.
	 * @return the class types of beans to check
	 */
	Class<?>[] value() default {};

	/**
	 * The class type names of bean that should be checked. The condition matches when
	 * each class specified is missing in the {@link ApplicationContext}.
	 * @return the class type names of beans to check
	 */
	String[] type() default {};

	/**
	 * The class type of beans that should be ignored when identifying matching beans.
	 * @return the class types of beans to ignore
	 * @since 1.2.5
	 */
	Class<?>[] ignored() default {};

	/**
	 * The class type names of beans that should be ignored when identifying matching
	 * beans.
	 * @return the class type names of beans to ignore
	 * @since 1.2.5
	 */
	String[] ignoredType() default {};

	/**
	 * The annotation type decorating a bean that should be checked. The condition matches
	 * when each annotation specified is missing from all beans in the
	 * {@link ApplicationContext}.
	 * @return the class-level annotation types to check
	 */
	Class<? extends Annotation>[] annotation() default {};

	/**
	 * The names of beans to check. The condition matches when each bean name specified is
	 * missing in the {@link ApplicationContext}.
	 * @return the name of beans to check
	 */
	String[] name() default {};

	/**
	 * Strategy to decide if the application context hierarchy (parent contexts) should be
	 * considered.
	 * @return the search strategy
	 */
	SearchStrategy search() default SearchStrategy.ALL;

}
