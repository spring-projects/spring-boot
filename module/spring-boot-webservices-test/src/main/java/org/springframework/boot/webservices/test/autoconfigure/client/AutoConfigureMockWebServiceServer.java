/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.client;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.PropertyMapping;
import org.springframework.ws.test.client.MockWebServiceServer;

/**
 * Annotation that can be applied to a test class to enable and configure
 * auto-configuration of a single {@link MockWebServiceServer}.
 *
 * @author Dmytro Nosan
 * @since 2.3.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
@PropertyMapping("spring.test.webservice.client.mockserver")
public @interface AutoConfigureMockWebServiceServer {

	/**
	 * If {@link MockWebServiceServer} bean should be registered. Defaults to
	 * {@code true}.
	 * @return if mock support is enabled
	 */
	boolean enabled() default true;

}
