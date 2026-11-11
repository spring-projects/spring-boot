/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.service.connection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslOptions;

/**
 * Configures the {@link SslOptions}, {@link SslBundleKey @SslBundleKey} and
 * {@link SslBundle#getProtocol() protocol} to use with an {@link SslBundle SSL} supported
 * {@link ServiceConnection @ServiceConnection}.
 * <p>
 * Also serves as a signal to enable automatic {@link SslBundle} extraction from supported
 * containers.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
public @interface Ssl {

	/**
	 * The protocol to use for the SSL connection.
	 * @return the SSL protocol
	 * @see SslBundle#getProtocol()
	 */
	String protocol() default SslBundle.DEFAULT_PROTOCOL;

	/**
	 * The ciphers that can be used for the SSL connection.
	 * @return the SSL ciphers
	 * @see SslOptions#getCiphers()
	 */
	String[] ciphers() default {};

	/**
	 * The protocols that are enabled for the SSL connection.
	 * @return the enabled SSL protocols
	 * @see SslOptions#getEnabledProtocols()
	 */
	String[] enabledProtocols() default {};

	/**
	 * The password that should be used to access the key.
	 * @return the key password
	 * @see SslBundleKey#getPassword()
	 */
	String keyPassword() default "";

	/**
	 * The alias that should be used to access the key.
	 * @return the key alias
	 * @see SslBundleKey#getAlias()
	 */
	String keyAlias() default "";

}
