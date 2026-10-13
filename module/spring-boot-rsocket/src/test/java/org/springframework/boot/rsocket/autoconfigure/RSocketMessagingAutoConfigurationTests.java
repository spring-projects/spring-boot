/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rsocket.autoconfigure;

import io.rsocket.frame.FrameType;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketFrameTypeMessageCondition;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeType;
import org.springframework.util.RouteMatcher;
import org.springframework.web.bind.annotation.ControllerAdvice;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RSocketMessagingAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 */
class RSocketMessagingAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RSocketMessagingAutoConfiguration.class))
		.withUserConfiguration(BaseConfiguration.class);

	@Test
	void shouldCreateDefaultBeans() {
		this.contextRunner.run((context) -> assertThat(context).getBeans(RSocketMessageHandler.class).hasSize(1));
	}

	@Test
	void shouldFailOnMissingStrategies() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(RSocketMessagingAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasFailed();
				Throwable startupFailure = context.getStartupFailure();
				assertThat(startupFailure).isNotNull();
				assertThat(startupFailure.getMessage()).contains("No qualifying bean of type "
						+ "'org.springframework.messaging.rsocket.RSocketStrategies' available");
			});
	}

	@Test
	void shouldUseCustomSocketAcceptor() {
		this.contextRunner.withUserConfiguration(CustomMessageHandler.class)
			.run((context) -> assertThat(context).getBeanNames(RSocketMessageHandler.class)
				.containsOnly("customMessageHandler"));
	}

	@Test
	void shouldApplyMessageHandlerCustomizers() {
		this.contextRunner.withUserConfiguration(CustomizerConfiguration.class).run((context) -> {
			RSocketMessageHandler handler = context.getBean(RSocketMessageHandler.class);
			assertThat(handler.getDefaultDataMimeType()).isEqualTo(MimeType.valueOf("application/json"));
		});
	}

	@Test
	void shouldRegisterControllerAdvice() {
		this.contextRunner.withBean(TestControllerAdvice.class).withBean(TestController.class).run((context) -> {
			RSocketMessageHandler handler = context.getBean(RSocketMessageHandler.class);
			MessageHeaderAccessor headers = new MessageHeaderAccessor();
			RouteMatcher routeMatcher = handler.getRouteMatcher();
			assertThat(routeMatcher).isNotNull();
			RouteMatcher.Route route = routeMatcher.parseRoute("exception");
			headers.setHeader(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER, route);
			headers.setHeader(RSocketFrameTypeMessageCondition.FRAME_TYPE_HEADER, FrameType.REQUEST_FNF);
			Message<?> message = MessageBuilder.createMessage(Mono.empty(), headers.getMessageHeaders());

			StepVerifier.create(handler.handleMessage(message)).expectComplete().verify();
			assertThat(context.getBean(TestControllerAdvice.class).isExceptionHandled()).isTrue();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		RSocketStrategies rSocketStrategies() {
			return RSocketStrategies.builder()
				.encoder(CharSequenceEncoder.textPlainOnly())
				.decoder(StringDecoder.allMimeTypes())
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomMessageHandler {

		@Bean
		RSocketMessageHandler customMessageHandler() {
			RSocketMessageHandler messageHandler = new RSocketMessageHandler();
			RSocketStrategies strategies = RSocketStrategies.builder()
				.encoder(CharSequenceEncoder.textPlainOnly())
				.decoder(StringDecoder.allMimeTypes())
				.build();
			messageHandler.setRSocketStrategies(strategies);
			return messageHandler;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfiguration {

		@Bean
		RSocketMessageHandlerCustomizer customizer() {
			return (messageHandler) -> messageHandler.setDefaultDataMimeType(MimeType.valueOf("application/json"));
		}

	}

	@Controller
	static final class TestController {

		@MessageMapping("exception")
		void handleWithSimulatedException() {
			throw new IllegalStateException("simulated exception");
		}

	}

	@ControllerAdvice
	static final class TestControllerAdvice {

		boolean exceptionHandled;

		boolean isExceptionHandled() {
			return this.exceptionHandled;
		}

		@MessageExceptionHandler
		void handleException(IllegalStateException ex) {
			this.exceptionHandled = true;
		}

	}

}
