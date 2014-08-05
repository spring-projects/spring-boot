/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.context.properties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for externalized configuration. Add this to a class definition if you want
 * to bind and validate some external Properties (e.g. from a .properties file).
 *
 * @author Dave Syer
 * @see ConfigurationPropertiesBindingPostProcessor
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigurationProperties {

	/**
	 * The name prefix of the properties that are valid to bind to this object. Synonym
	 * for {@link #prefix()}.
	 * @return the name prefix of the properties to bind
	 */
	String value() default "";

	/**
	 * The name prefix of the properties that are valid to bind to this object. Synonym
	 * for {@link #value()}.
	 * @return the name prefix of the properties to bind
	 */
	String prefix() default "";

	/**
	 * Flag to indicate that when binding to this object invalid fields should be ignored.
	 * Invalid means invalid according to the binder that is used, and usually this means
	 * fields of the wrong type (or that cannot be coerced into the correct type).
	 * @return the flag value (default false)
	 */
	boolean ignoreInvalidFields() default false;

	/**
	 * Flag to indicate that when binding to this object fields with periods in their
	 * names should be ignored.
	 * @return the flag value (default false)
	 */
	boolean ignoreNestedProperties() default false;

	/**
	 * Flag to indicate that when binding to this object unknown fields should be ignored.
	 * An unknown field could be a sign of a mistake in the Properties.
	 * @return the flag value (default true)
	 */
	boolean ignoreUnknownFields() default true;

	/**
	 * Flag to indicate that validation errors can be swallowed. If set they will be
	 * logged, but not propagate to the caller.
	 * @return the flag value (default true)
	 */
	boolean exceptionIfInvalid() default true;

	/**
	 * Optionally provide explicit resource locations to bind to. By default the
	 * configuration at these specified locations will be merged with the default
	 * configuration.
	 * @return the path (or paths) of resources to bind to
	 * @see #merge()
	 */
	String[] locations() default {};

	/**
	 * Flag to indicate that configuration loaded from the specified locations should be
	 * merged with the default configuration.
	 * @return the flag value (default true)
	 * @see #locations()
	 */
	boolean merge() default true;

}
