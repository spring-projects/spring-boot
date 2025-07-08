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

package org.springframework.boot.testcontainers.service.connection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.core.annotation.AliasFor;

/**
 * Configures the {@link PemSslStoreBundle} key store to use with an {@link SslBundle SSL}
 * supported {@link ServiceConnection @ServiceConnection}.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
public @interface PemKeyStore {

	/**
	 * Alias for {@link #certificate()}.
	 * @return the store certificate
	 */
	@AliasFor("certificate")
	String value() default "";

	/**
	 * The location or content of the certificate or certificate chain in PEM format.
	 * @return the store certificate location or content
	 */
	@AliasFor("value")
	String certificate() default "";

	/**
	 * The location or content of the private key in PEM format.
	 * @return the store private key location or content
	 */
	String privateKey() default "";

	/**
	 * The password used to decrypt an encrypted private key.
	 * @return the store private key password
	 */
	String privateKeyPassword() default "";

	/**
	 * The type of the store to create, e.g. JKS.
	 * @return the store type
	 */
	String type() default "";

}
