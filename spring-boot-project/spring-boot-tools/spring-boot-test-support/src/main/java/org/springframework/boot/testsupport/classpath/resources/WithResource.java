/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
