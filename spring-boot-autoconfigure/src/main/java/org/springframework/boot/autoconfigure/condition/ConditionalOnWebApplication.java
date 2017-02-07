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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional} that matches when the application is a web application. By default,
 * any web application will match but it can be narrowed using the {@link #type()}
 * attribute.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnWebApplicationCondition.class)
public @interface ConditionalOnWebApplication {

	/**
	 * The required type of the web application.
	 * @return the required web application type
	 */
	Type type() default Type.ANY;

	/**
	 * Available application types.
	 */
	enum Type {

		/**
		 * Any web application will match.
		 */
		ANY,

		/**
		 * Only servlet-based web application will match.
		 */
		SERVLET,

		/**
		 * Only reactive-based web application will match.
		 */
		REACTIVE

	}

}
