/*
 * Copyright 2012-2017 the original author or authors.
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
 * Annotation to be used on accessor methods or fields when the class is annotated with
 * {@link ConfigurationProperties}. It allows to specify additional metadata for a single
 * property.
 * <p>
 * Annotating a method that is not a getter or setter in the sense of the JavaBeans spec
 * will cause an exception.
 *
 * @author Tom Hombergs
 * @since 2.0.0
 * @see ConfigurationProperties
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigurationPropertyValue {

	/**
	 * Name of the property whose value to use if the property with the name of the
	 * annotated field itself is not defined.
	 * <p>
	 * The fallback property name has to be specified including potential prefixes defined
	 * in {@link ConfigurationProperties} annotations.
	 *
	 * @return the name of the fallback property
	 */
	String fallback() default "";

}
