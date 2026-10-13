/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.classpath.resources;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that indicates that the content of a resource should be injected. Supported
 * on parameters of type:
 *
 * <ul>
 * <li>{@link String}</li>
 * </ul>
 *
 * @author Andy Wilkinson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ResourceContent {

	/**
	 * The name of the resource whose content should be injected.
	 * @return the resource name
	 */
	String value();

}
