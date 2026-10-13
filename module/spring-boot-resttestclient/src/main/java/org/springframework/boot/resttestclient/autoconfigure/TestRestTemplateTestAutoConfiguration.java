/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.resttestclient.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.TestRestTemplate.HttpClientOption;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.boot.test.http.server.LocalTestWebServer.Scheme;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Test auto-configuration for {@link TestRestTemplate}.
 *
 * @author Andy Wilkinson
 * @see AutoConfigureTestRestTemplate
 */
@AutoConfiguration
final class TestRestTemplateTestAutoConfiguration {

	@Bean(name = "org.springframework.boot.resttestclient.TestRestTemplate")
	@ConditionalOnMissingBean
	TestRestTemplate testRestTemplate(ObjectProvider<RestTemplateBuilder> builderProvider,
			ApplicationContext applicationContext) {
		RestTemplateBuilder builder = builderProvider.getIfAvailable(RestTemplateBuilder::new);
		LocalTestWebServer localTestWebServer = LocalTestWebServer.obtain(applicationContext);
		TestRestTemplate template = new TestRestTemplate(builder, null, null,
				httpClientOptions(localTestWebServer.scheme()));
		template.setUriTemplateHandler(localTestWebServer.uriBuilderFactory());
		return template;
	}

	private HttpClientOption[] httpClientOptions(Scheme scheme) {
		return switch (scheme) {
			case HTTP -> new HttpClientOption[] {};
			case HTTPS -> new HttpClientOption[] { HttpClientOption.SSL };
		};
	}

}
