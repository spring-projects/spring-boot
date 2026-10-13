/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that matches when the application is a web
 * application. By default, any web application will match but it can be narrowed using
 * the {@link #type()} attribute.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @since 1.0.0
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
