package org.springframework.boot.docs.io.grpc.client.channelcustomization

import io.grpc.ManagedChannelBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer
import org.springframework.grpc.client.interceptor.security.BasicAuthenticationInterceptor
import java.util.function.Consumer

@Configuration(proxyBeanMethods = false)
class MyGrpcConfiguration {

	@Bean
	fun helloChannelCustomizer(): GrpcChannelBuilderCustomizer<*> {
		return GrpcChannelBuilderCustomizer.matching("hello", { builder ->
			builder.intercept(BasicAuthenticationInterceptor("user", "password"))
		})
	}

}
