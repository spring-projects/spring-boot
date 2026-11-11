/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.web.client.RestClient.Builder;

/**
 * Annotation that can be applied to a test class to enable auto-configuration of a
 * {@link Builder WebClient.Builder}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigureJson
@ImportAutoConfiguration
public @interface AutoConfigureWebClient {

}
