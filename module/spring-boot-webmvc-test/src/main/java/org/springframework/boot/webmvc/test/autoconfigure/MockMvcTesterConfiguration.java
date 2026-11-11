/*
 * Copyright 2012-present the original author or authors.
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
