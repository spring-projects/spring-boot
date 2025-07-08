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

package org.springframework.boot.webflux.autoconfigure.actuate.endpoint.web;

import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.integrationtest.AbstractHealthEndpointAdditionalPathIntegrationTests;
import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.reactor.netty.autoconfigure.NettyReactiveWebServerAutoConfiguration;
import org.springframework.boot.reactor.netty.autoconfigure.actuate.web.server.NettyReactiveManagementContextAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.context.reactive.ConfigurableReactiveWebApplicationContext;
import org.springframework.boot.web.server.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.web.server.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.webflux.autoconfigure.HttpHandlerAutoConfiguration;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;

/**
 * Integration tests for Webflux health groups on an additional path.
 *
 * @author Madhura Bhave
 */
class WebFluxHealthEndpointAdditionalPathIntegrationTests extends
		AbstractHealthEndpointAdditionalPathIntegrationTests<ReactiveWebApplicationContextRunner, ConfigurableReactiveWebApplicationContext, AssertableReactiveWebApplicationContext> {

	WebFluxHealthEndpointAdditionalPathIntegrationTests() {
		super(new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
			.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, CodecsAutoConfiguration.class,
					WebFluxAutoConfiguration.class, HealthContributorAutoConfiguration.class,
					HealthContributorRegistryAutoConfiguration.class, HttpHandlerAutoConfiguration.class,
					EndpointAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
					WebFluxHealthEndpointExtensionAutoConfiguration.class,
					DiskSpaceHealthContributorAutoConfiguration.class, WebEndpointAutoConfiguration.class,
					ManagementContextAutoConfiguration.class, NettyReactiveWebServerAutoConfiguration.class,
					NettyReactiveManagementContextAutoConfiguration.class, BeansEndpointAutoConfiguration.class))
			.withInitializer(new ServerPortInfoApplicationContextInitializer())
			.withPropertyValues("server.port=0"));
	}

}
