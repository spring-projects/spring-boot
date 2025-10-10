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

package org.springframework.boot.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;

/**
 * Indicates that a class provides configuration that can be automatically applied by
 * Spring Boot tests. Test Auto-configuration classes are regular
 * {@link AutoConfiguration @AutoConfiguration} classes but may be package-private.
 *
 * @author Phillip Webb
 * @see AutoConfiguration
 * @since 4.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration(proxyBeanMethods = false)
@AutoConfiguration
public @interface TestAutoConfiguration {

	/**
	 * Alias for {@link AutoConfiguration#value()}.
	 * @return the aliased value
	 */
	@AliasFor(annotation = AutoConfiguration.class)
	String value() default "";

	/**
	 * Alias for {@link AutoConfiguration#before()}.
	 * @return the aliased value
	 */
	@AliasFor(annotation = AutoConfiguration.class)
	Class<?>[] before() default {};

	/**
	 * Alias for {@link AutoConfiguration#beforeName()}.
	 * @return the aliased value
	 */
	@AliasFor(annotation = AutoConfiguration.class)
	String[] beforeName() default {};

	/**
	 * Alias for {@link AutoConfiguration#after()}.
	 * @return the aliased value
	 */
	@AliasFor(annotation = AutoConfiguration.class)
	Class<?>[] after() default {};

	/**
	 * Alias for {@link AutoConfiguration#afterName()}.
	 * @return the aliased value
	 */
	@AliasFor(annotation = AutoConfiguration.class)
	String[] afterName() default {};

}
