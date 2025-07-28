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

package org.springframework.boot.undertow.autoconfigure.reactive;

import io.undertow.Undertow;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.undertow.UndertowBuilderCustomizer;
import org.springframework.boot.undertow.autoconfigure.UndertowServerProperties;
import org.springframework.boot.undertow.autoconfigure.UndertowWebServerConfiguration;
import org.springframework.boot.undertow.reactive.UndertowReactiveWebServerFactory;
import org.springframework.boot.web.server.autoconfigure.reactive.ReactiveWebServerConfiguration;
import org.springframework.boot.web.server.reactive.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ReactiveHttpInputMessage;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for an Undertow-based reactive web
 * server.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ ReactiveHttpInputMessage.class, Undertow.class })
@ConditionalOnWebApplication(type = Type.REACTIVE)
@EnableConfigurationProperties(UndertowServerProperties.class)
@Import({ UndertowWebServerConfiguration.class, ReactiveWebServerConfiguration.class })
public final class UndertowReactiveWebServerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ReactiveWebServerFactory.class)
	UndertowReactiveWebServerFactory undertowReactiveWebServerFactory(
			ObjectProvider<UndertowBuilderCustomizer> builderCustomizers) {
		UndertowReactiveWebServerFactory factory = new UndertowReactiveWebServerFactory();
		factory.getBuilderCustomizers().addAll(builderCustomizers.orderedStream().toList());
		return factory;
	}

}
