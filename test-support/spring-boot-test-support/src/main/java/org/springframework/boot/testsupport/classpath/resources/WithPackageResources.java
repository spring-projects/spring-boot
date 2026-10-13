/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.classpath.resources;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Makes resources in the package of the annotated class available from the root of the
 * classpath.
 *
 * @author Andy Wilkinson
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@ExtendWith(ResourcesExtension.class)
public @interface WithPackageResources {

	/**
	 * The resources to make available from the root.
	 * @return the resources
	 */
	String[] value();

}
