/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.testcontainers.service.connection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.core.annotation.AliasFor;

/**
 * Configures the {@link JksSslStoreBundle} trust store to use with an {@link SslBundle
 * SSL} supported {@link ServiceConnection @ServiceConnection}.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
public @interface JksTrustStore {

	/**
	 * Alias for {@link #location()}.
	 * @return the store location
	 */
	@AliasFor("location")
	String value() default "";

	/**
	 * The location of the resource containing the store content.
	 * @return the store location
	 */
	@AliasFor("value")
	String location() default "";

	/**
	 * The password used to access the store.
	 * @return the store password
	 */
	String password() default "";

	/**
	 * The type of the store to create, e.g. JKS.
	 * @return store type
	 */
	String type() default "";

	/**
	 * The provider for the store.
	 * @return the store provider
	 */
	String provider() default "";

}
