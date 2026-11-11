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
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Annotation that can be applied to a test class to enable and configure
 * auto-configuration of web service clients.
 *
 * @author Dmytro Nosan
 * @since 2.3.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
@PropertyMapping("spring.test.webservice.client")
public @interface AutoConfigureWebServiceClient {

	/**
	 * If a {@link WebServiceTemplate} bean should be registered. Defaults to
	 * {@code false} with the assumption that the {@link WebServiceTemplateBuilder} will
	 * be used.
	 * @return if a {@link WebServiceTemplate} bean should be added.
	 */
	boolean registerWebServiceTemplate() default false;

}
