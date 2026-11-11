/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure;

import org.htmlunit.WebClient;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.web.htmlunit.UriBuilderFactoryWebClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebClientBuilder;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * Auto-configuration for HtmlUnit {@link WebClient} MockMVC integration.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = MockMvcAutoConfiguration.class)
@ConditionalOnClass(WebClient.class)
@ConditionalOnBooleanProperty(name = "spring.test.mockmvc.htmlunit.webclient.enabled", matchIfMissing = true)
public final class MockMvcWebClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean({ WebClient.class, MockMvcWebClientBuilder.class })
	@ConditionalOnBean(MockMvc.class)
	MockMvcWebClientBuilder mockMvcWebClientBuilder(Environment environment, MockMvc mockMvc) {
		String url = environment.getProperty("spring.test.mockmvc.htmlunit.url", "http://localhost");
		return MockMvcWebClientBuilder.mockMvcSetup(mockMvc)
			.withDelegate(new UriBuilderFactoryWebClient(new DefaultUriBuilderFactory(url)));
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(MockMvcWebClientBuilder.class)
	WebClient htmlUnitWebClient(MockMvcWebClientBuilder builder) {
		return builder.build();
	}

}
