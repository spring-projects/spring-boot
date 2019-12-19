/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

/**
 * Import and apply the specified configuration classes after auto-configuration classes.
 *
 * @author Tadaya Tsuyukubo
 * @since 2.3.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ImportAfterAutoConfigurationDeferredImportSelector.class)
public @interface ImportAfterAutoConfiguration {

	/**
	 * The configuration classes that should be imported after auto-configurations. This
	 * is an alias for {@link #classes()}.
	 * @return the classes to import
	 */
	@AliasFor("classes")
	Class<?>[] value() default {};

	/**
	 * The configuration classes that should be imported after auto-configurations.
	 * @return the classes to import
	 */
	@AliasFor("value")
	Class<?>[] classes() default {};

}
