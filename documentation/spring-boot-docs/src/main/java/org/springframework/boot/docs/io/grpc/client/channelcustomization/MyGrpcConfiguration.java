/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.grpc.client.channelcustomization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.interceptor.security.BasicAuthenticationInterceptor;

@Configuration(proxyBeanMethods = false)
public class MyGrpcConfiguration {

	@Bean
	GrpcChannelBuilderCustomizer<?> helloChannelCustomizer() {
		return GrpcChannelBuilderCustomizer.matching("hello",
				(builder) -> builder.intercept(new BasicAuthenticationInterceptor("user", "password")));
	}

}
