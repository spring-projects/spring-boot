/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.classpath.resources;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Makes a resource available from the thread context class loader. Typically used when a
 * test requires an empty directory to exist.
 *
 * @author Andy Wilkinson
 */
@Inherited
@Repeatable(WithResourceDirectories.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@ExtendWith(ResourcesExtension.class)
public @interface WithResourceDirectory {

	/**
	 * The name of the directory.
	 * @return the name
	 */
	String value();

}
