/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.webservices.client;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.properties.PropertyMapping;
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
