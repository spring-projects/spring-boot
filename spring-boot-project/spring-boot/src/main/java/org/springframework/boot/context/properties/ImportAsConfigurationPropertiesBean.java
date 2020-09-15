/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.properties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

/**
 * Imports classes as {@link ConfigurationProperties @ConfigurationProperties} beans. Can
 * be used to import {@link ConfigurationProperties @ConfigurationProperties} annotated
 * types or third-party classes as configuration property beans.
 * <p>
 * Classes imported via this annotation that have a default constructor will use
 * {@code setter} binding, those with a non-default constructor will use
 * {@link ConstructorBinding @ConstructorBinding}. If you are looking to inject beans into
 * a constructor, you should use a regular {@link Configuration @Configuration} class
 * {@code @Bean} method instead.
 * <p>
 * The {@code @ConfigurationProperties} alias attributes defined on this class will only
 * be used if the imported class is not itself annotated
 * with{@code @ConfigurationProperties}.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableConfigurationProperties
@ConfigurationProperties
@Repeatable(ImportAsConfigurationPropertiesBeans.class)
@Import(ImportAsConfigurationPropertiesBeanRegistrar.class)
public @interface ImportAsConfigurationPropertiesBean {

	/**
	 * One or more types that should be imported as a bean.
	 * @return the types to import
	 */
	@AliasFor("type")
	Class<?>[] value() default {};

	/**
	 * One or more types that should be imported as a bean.
	 * @return the types to import
	 */
	@AliasFor("value")
	Class<?>[] type() default {};

	/**
	 * The prefix of the properties that are valid to bind to this object. A valid prefix
	 * is defined by one or more words separated with dots (e.g.
	 * {@code "acme.system.feature"}).
	 * @return the prefix of the properties to bind
	 * @see ConfigurationProperties#prefix()
	 */
	@AliasFor(annotation = ConfigurationProperties.class)
	String prefix() default "";

	/**
	 * Flag to indicate that when binding to this object invalid fields should be ignored.
	 * Invalid means invalid according to the binder that is used, and usually this means
	 * fields of the wrong type (or that cannot be coerced into the correct type).
	 * @return the flag value (default false)
	 * @see ConfigurationProperties#ignoreInvalidFields()
	 */
	@AliasFor(annotation = ConfigurationProperties.class)
	boolean ignoreInvalidFields() default false;

	/**
	 * Flag to indicate that when binding to this object unknown fields should be ignored.
	 * An unknown field could be a sign of a mistake in the Properties.
	 * @return the flag value (default true)
	 * @see ConfigurationProperties#ignoreUnknownFields()
	 */
	@AliasFor(annotation = ConfigurationProperties.class)
	boolean ignoreUnknownFields() default true;

}
