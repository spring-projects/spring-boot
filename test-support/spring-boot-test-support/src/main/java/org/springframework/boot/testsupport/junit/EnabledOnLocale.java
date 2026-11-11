/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code @EnabledOnLocale} annotation is used to conditionally enable a test method based
 * on the specified locale attributes.
 *
 * @author Dmytro Nosan
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnabledOnLocaleCondition.class)
public @interface EnabledOnLocale {

	/**
	 * Specifies the language code for which the test method should be enabled.
	 * @return the language code
	 */
	String language();

}
