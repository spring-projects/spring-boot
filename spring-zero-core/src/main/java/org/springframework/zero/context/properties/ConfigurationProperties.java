/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.zero.context.properties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for externalized configuration. Add this to a class definition if you want
 * to bind and validate some external Properties (e.g. from a .properties file).
 * 
 * @see ConfigurationPropertiesBindingPostProcessor
 * 
 * @author Dave Syer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigurationProperties {

	/**
	 * The (optional) name of the object to be bound. Properties to bind can have a name
	 * prefix to select the properties that are valid to this object. Synonym for
	 * {@link #name()}.
	 * 
	 * @return the name prefix of the properties to bind
	 */
	String value() default "";

	/**
	 * The (optional) name of the object to be bound. Properties to bind can have a name
	 * prefix to select the properties that are valid to this object. Synonym for
	 * {@link #value()}.
	 * 
	 * @return the name prefix of the properties to bind
	 */
	String name() default "";

	/**
	 * Flag to indicate that when binding to this object invalid fields should be ignored.
	 * Invalid means invalid according to the binder that is used, and usually this means
	 * fields of the wrong type (or that cannot be coerced into the correct type).
	 * 
	 * @return the flag value (default false)
	 */
	boolean ignoreInvalidFields() default false;

	/**
	 * Flag to indicate that when binding to this object unknown fields should be ignored.
	 * An unknown field could be a sign of a mistake in the Properties.
	 * 
	 * @return the flag value (default true)
	 */
	boolean ignoreUnknownFields() default true;

}
