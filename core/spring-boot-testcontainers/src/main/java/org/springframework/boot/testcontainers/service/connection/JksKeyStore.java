/*
 * Copyright 2012-present the original author or authors.
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
 * Configures the {@link JksSslStoreBundle} key store to use with an {@link SslBundle SSL}
 * supported {@link ServiceConnection @ServiceConnection}.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
public @interface JksKeyStore {

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
