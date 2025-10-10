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

package org.springframework.boot.restclient.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer;
import org.springframework.boot.restclient.test.MockServerRestTemplateCustomizer;
import org.springframework.boot.test.context.PropertyMapping;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient.Builder;

/**
 * Annotation that can be applied to a test class to enable and configure
 * auto-configuration of a single {@link MockRestServiceServer}. Only useful when a single
 * call is made to {@link RestTemplateBuilder} or {@link Builder RestClient.Builder}. If
 * multiple {@link org.springframework.web.client.RestTemplate RestTemplates} or
 * {@link org.springframework.web.client.RestClient RestClients} are in use, inject a
 * {@link MockServerRestTemplateCustomizer} and use
 * {@link MockServerRestTemplateCustomizer#getServer(org.springframework.web.client.RestTemplate)
 * getServer(RestTemplate)}, or inject a {@link MockServerRestClientCustomizer} and use
 * {@link MockServerRestClientCustomizer#getServer(org.springframework.web.client.RestClient.Builder)
 * * getServer(RestClient.Builder)}, or bind a {@link MockRestServiceServer} directly.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 4.0.0
 * @see MockServerRestTemplateCustomizer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
@PropertyMapping("spring.test.webclient.mockrestserviceserver")
public @interface AutoConfigureMockRestServiceServer {

	/**
	 * If {@link MockServerRestTemplateCustomizer} and
	 * {@link MockServerRestClientCustomizer} should be enabled and
	 * {@link MockRestServiceServer} beans should be registered. Defaults to {@code true}
	 * @return if mock support is enabled
	 */
	boolean enabled() default true;

}
