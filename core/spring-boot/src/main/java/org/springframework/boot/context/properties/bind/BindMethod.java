/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.bind;

/**
 * Configuration property binding methods.
 *
 * @author Andy Wilkinson
 * @since 3.0.8
 */
public enum BindMethod {

	/**
	 * Java Bean using getter/setter binding.
	 */
	JAVA_BEAN,

	/**
	 * Value object using constructor binding.
	 */
	VALUE_OBJECT

}
