/*
 * Copyright 2012-present the original author or authors.
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
