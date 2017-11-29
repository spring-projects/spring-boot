/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure.web.client;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.properties.PropertyMapping;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.web.client.MockRestServiceServer;

/**
 * Annotation that can be applied to a test class to enable and configure
 * auto-configuration of a single {@link MockRestServiceServer}. Only useful when a single
 * call is made to {@link RestTemplateBuilder}. If multiple
 * {@link org.springframework.web.client.RestTemplate RestTemplates} are in use, inject
 * {@link MockServerRestTemplateCustomizer} and use
 * {@link MockServerRestTemplateCustomizer#getServer(org.springframework.web.client.RestTemplate)
 * getServer(RestTemplate)} or bind a {@link MockRestServiceServer} directly.
 *
 * @author Phillip Webb
 * @since 1.4.0
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
	 * If {@link MockServerRestTemplateCustomizer} should be enabled and
	 * {@link MockRestServiceServer} beans should be registered. Defaults to {@code true}
	 * @return if mock support is enabled
	 */
	boolean enabled() default true;

}
