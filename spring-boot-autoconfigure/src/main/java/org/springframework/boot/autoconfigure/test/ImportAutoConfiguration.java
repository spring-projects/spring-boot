/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Import and apply the selected auto-configuration classes for testing purposes. Applies
 * the same ordering rules as {@code @EnableAutoConfiguration} but restricts the
 * auto-configuration classes to the specified set, rather than consulting
 * {@code spring.factories}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @deprecated as of 1.4 in favor of
 * {@link org.springframework.boot.autoconfigure.ImportAutoConfiguration}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@org.springframework.boot.autoconfigure.ImportAutoConfiguration({})
@Deprecated
public @interface ImportAutoConfiguration {

	/**
	 * The auto-configuration classes that should be imported.
	 * @return the classes to import
	 */
	@AliasFor(annotation = org.springframework.boot.autoconfigure.ImportAutoConfiguration.class, attribute = "value")
	Class<?>[] value();

}
