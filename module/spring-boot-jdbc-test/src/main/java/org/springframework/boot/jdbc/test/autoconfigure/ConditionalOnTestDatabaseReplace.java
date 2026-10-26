/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that matches when {@code spring.test.database.replace}
 * is bound to the specified {@link Replace} value. When the property is missing, empty,
 * or blank, {@link Replace#NON_TEST} is assumed.
 *
 * @author Hyunwoo Jung
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnTestDatabaseReplaceCondition.class)
@interface ConditionalOnTestDatabaseReplace {

	/**
	 * The {@link Replace} value that the property must be bound to.
	 * @return the required replace value
	 */
	Replace value();

}
