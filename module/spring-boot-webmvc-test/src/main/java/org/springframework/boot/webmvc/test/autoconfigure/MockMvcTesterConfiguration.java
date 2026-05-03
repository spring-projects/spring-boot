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

package org.springframework.boot.webmvc.test.autoconfigure;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Configuration for {@link MockMvcTester}.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.assertj.core.api.Assert")
class MockMvcTesterConfiguration {

	@Bean
	@ConditionalOnMissingBean
	MockMvcTester mockMvcTester(MockMvc mockMvc,
			ObjectProvider<ServerHttpMessageConvertersCustomizer> customizersProvider) {
		MockMvcTester mockMvcTester = MockMvcTester.create(mockMvc);
		List<ServerHttpMessageConvertersCustomizer> customizers = customizersProvider.orderedStream().toList();
		if (!customizers.isEmpty()) {
			ServerBuilder serverBuilder = HttpMessageConverters.forServer();
			customizersProvider.orderedStream().forEach((customizer) -> customizer.customize(serverBuilder));
			mockMvcTester = mockMvcTester.withHttpMessageConverters(serverBuilder.build());
		}
		return mockMvcTester;
	}

}
