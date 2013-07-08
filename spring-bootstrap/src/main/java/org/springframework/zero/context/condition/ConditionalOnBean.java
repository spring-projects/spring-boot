/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.zero.context.condition;

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
 * already contained in the {@link BeanFactory}.
 * 
 * @author Phillip Webb
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnBeanCondition.class)
public @interface ConditionalOnBean {

	/**
	 * The class type of bean that should be checked. The condition matches when any of
	 * the classes specified is contained in the {@link ApplicationContext}.
	 * @return the class types of beans to check
	 */
	Class<?>[] value() default {};

	/**
	 * The names of beans to check. The condition matches when any of the bean names
	 * specified is contained in the {@link ApplicationContext}.
	 * @return the name of beans to check
	 */
	String[] name() default {};

	/**
	 * If the application context hierarchy (parent contexts) should be considered.
	 */
	boolean considerHierarchy() default true;

}
