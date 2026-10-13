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
 * Makes a resource directory available from the thread context class loader.
 * <p>
 * For cases where one resource needs to refer to another, the resource's content may
 * contain the placeholder <code>${resourceRoot}</code>. It will be replaced with the path
 * to the root of the resources. For example, a resource with the {@link #name}
 * {@code example.txt} can be referenced using <code>${resourceRoot}/example.txt</code>.
 *
 * @author Andy Wilkinson
 */
@Inherited
@Repeatable(WithResources.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@ExtendWith(ResourcesExtension.class)
public @interface WithResource {

	/**
	 * The name of the resource.
	 * @return the name
	 */
	String name();

	/**
	 * The content of the resource. When omitted an empty resource will be created.
	 * @return the content
	 */
	String content() default "";

	/**
	 * Whether the resource should be available in addition to those that are already on
	 * the classpath are instead of any existing resources with the same name.
	 * @return whether this is an additional resource
	 */
	boolean additional() default true;

}
