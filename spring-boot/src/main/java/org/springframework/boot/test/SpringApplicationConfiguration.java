/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ContextConfiguration;

/**
 * Class-level annotation that is used to determine how to load and configure an
 * {@code ApplicationContext} for integration tests.
 * <p>
 * Similar to the standard {@link ContextConfiguration @ContextConfiguration} but uses
 * Spring Boot's {@link SpringApplicationContextLoader}.
 *
 * @author Dave Syer
 * @author Sam Brannen
 * @see SpringApplicationContextLoader
 * @see ContextConfiguration
 */
@ContextConfiguration(loader = SpringApplicationContextLoader.class)
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SpringApplicationConfiguration {

	/**
	 * The context configuration classes.
	 * @see ContextConfiguration#classes()
	 * @return the context configuration classes
	 */
	@AliasFor("classes")
	Class<?>[] value() default {};

	/**
	 * The context configuration locations.
	 * @see ContextConfiguration#locations()
	 * @return the context configuration locations
	 */
	@AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
	String[] locations() default {};

	/**
	 * The context configuration classes.
	 * @see ContextConfiguration#classes()
	 * @return the context configuration classes
	 */
	@AliasFor("value")
	Class<?>[] classes() default {};

	/**
	 * The context configuration initializers.
	 * @see ContextConfiguration#initializers()
	 * @return the context configuration initializers
	 */
	@AliasFor(annotation = ContextConfiguration.class, attribute = "initializers")
	Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>[] initializers() default {};

	/**
	 * Should context locations be inherited.
	 * @see ContextConfiguration#inheritLocations()
	 * @return {@code true} if context locations should be inherited
	 */
	@AliasFor(annotation = ContextConfiguration.class, attribute = "inheritLocations")
	boolean inheritLocations() default true;

	/**
	 * Should initializers be inherited.
	 * @see ContextConfiguration#inheritInitializers()
	 * @return {@code true} if context initializers should be inherited
	 */
	@AliasFor(annotation = ContextConfiguration.class, attribute = "inheritInitializers")
	boolean inheritInitializers() default true;

	/**
	 * The name of the context hierarchy level.
	 * @see ContextConfiguration#name()
	 * @return the name of the context hierarchy level
	 */
	@AliasFor(annotation = ContextConfiguration.class, attribute = "name")
	String name() default "";

}
