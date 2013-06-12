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
package org.springframework.bootstrap.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.catalina.core.ApplicationContext;
import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional} that only matches specific {@link ApplicationContext}s and that can
 * optionally create them.
 * 
 * @author Phillip Webb
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnApplicationContextCondition.class)
public @interface ConditionalOnApplicationContext {

	// FIXME complete of delete this

	/**
	 * The ID of the application context.
	 */
	String value() default "";

	// FIXME Strategy Interface Class, eg ApplicationContextCondition
	// condition=SomethingSpecific.class

	boolean createIfMissing() default false;

}
