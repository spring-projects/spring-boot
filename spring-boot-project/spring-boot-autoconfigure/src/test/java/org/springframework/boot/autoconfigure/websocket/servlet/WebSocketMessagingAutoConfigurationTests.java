/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.websocket.servlet;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketMessageBrokerConfiguration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link WebSocketMessagingAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class WebSocketMessagingAutoConfigurationTests {

	private AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext();

	private SockJsClient sockJsClient;

	@BeforeEach
	void setup() {
		List<Transport> transports = Arrays.asList(
				new WebSocketTransport(new StandardWebSocketClient(new WsWebSocketContainer())),
				new RestTemplateXhrTransport(new RestTemplate()));
		this.sockJsClient = new SockJsClient(transports);
	}

	@AfterEach
	void tearDown() {
		if (this.context.isActive()) {
			this.context.close();
		}
		this.sockJsClient.stop();
	}

	@Test
	void basicMessagingWithJsonResponse() throws Throwable {
		Object result = performStompSubscription("/app/json");
		JSONAssert.assertEquals("{\"foo\" : 5,\"bar\" : \"baz\"}", new String((byte[]) result), true);
	}

	@Test
	void basicMessagingWithStringResponse() throws Throwable {
		Object result = performStompSubscription("/app/string");
		assertThat(new String((byte[]) result)).isEqualTo("string data");
	}

	@Test
	void whenLazyInitializationIsEnabledThenBasicMessagingWorks() throws Throwable {
		this.context.register(LazyInitializationBeanFactoryPostProcessor.class);
		Object result = performStompSubscription("/app/string");
		assertThat(new String((byte[]) result)).isEqualTo("string data");
	}

	@Test
	void customizedConverterTypesMatchDefaultConverterTypes() {
		List<MessageConverter> customizedConverters = getCustomizedConverters();
		List<MessageConverter> defaultConverters = getDefaultConverters();
		assertThat(customizedConverters.size()).isEqualTo(defaultConverters.size());
		Iterator<MessageConverter> customizedIterator = customizedConverters.iterator();
		Iterator<MessageConverter> defaultIterator = defaultConverters.iterator();
		while (customizedIterator.hasNext()) {
			assertThat(customizedIterator.next()).isInstanceOf(defaultIterator.next().getClass());
		}
	}

	private List<MessageConverter> getCustomizedConverters() {
		List<MessageConverter> customizedConverters = new ArrayList<>();
		WebSocketMessagingAutoConfiguration.WebSocketMessageConverterConfiguration configuration = new WebSocketMessagingAutoConfiguration.WebSocketMessageConverterConfiguration(
				new ObjectMapper());
		configuration.configureMessageConverters(customizedConverters);
		return customizedConverters;
	}

	private List<MessageConverter> getDefaultConverters() {
		DelegatingWebSocketMessageBrokerConfiguration configuration = new DelegatingWebSocketMessageBrokerConfiguration();
		CompositeMessageConverter compositeDefaultConverter = configuration.brokerMessageConverter();
		return compositeDefaultConverter.getConverters();
	}

	private Object performStompSubscription(String topic) throws Throwable {
		TestPropertyValues.of("server.port:0", "spring.jackson.serialization.indent-output:true").applyTo(this.context);
		this.context.register(WebSocketMessagingConfiguration.class);
		new ServerPortInfoApplicationContextInitializer().initialize(this.context);
		this.context.refresh();
		WebSocketStompClient stompClient = new WebSocketStompClient(this.sockJsClient);
		final AtomicReference<Throwable> failure = new AtomicReference<>();
		final AtomicReference<Object> result = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		StompSessionHandler handler = new StompSessionHandlerAdapter() {

			@Override
			public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
				session.subscribe(topic, new StompFrameHandler() {

					@Override
					public void handleFrame(StompHeaders headers, Object payload) {
						result.set(payload);
						latch.countDown();
					}

					@Override
					public Type getPayloadType(StompHeaders headers) {
						return Object.class;
					}

				});
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				latch.countDown();
			}

			@Override
			public void handleException(StompSession session, StompCommand command, StompHeaders headers,
					byte[] payload, Throwable exception) {
				failure.set(exception);
				latch.countDown();
			}

			@Override
			public void handleTransportError(StompSession session, Throwable exception) {
				failure.set(exception);
				latch.countDown();
			}

		};

		stompClient.setMessageConverter(new SimpleMessageConverter());
		stompClient.connect("ws://localhost:{port}/messaging", handler,
				this.context.getEnvironment().getProperty("local.server.port"));

		if (!latch.await(30, TimeUnit.SECONDS)) {
			if (failure.get() != null) {
				throw failure.get();
			}
			fail("Response was not received within 30 seconds");
		}
		return result.get();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebSocket
	@EnableConfigurationProperties
	@EnableWebSocketMessageBroker
	@ImportAutoConfiguration({ JacksonAutoConfiguration.class, ServletWebServerFactoryAutoConfiguration.class,
			WebSocketMessagingAutoConfiguration.class, DispatcherServletAutoConfiguration.class })
	static class WebSocketMessagingConfiguration implements WebSocketMessageBrokerConfigurer {

		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/messaging").withSockJS();
		}

		@Override
		public void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.setApplicationDestinationPrefixes("/app");
		}

		@Bean
		MessagingController messagingController() {
			return new MessagingController();
		}

		@Bean
		TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		TomcatWebSocketServletWebServerCustomizer tomcatCustomizer() {
			return new TomcatWebSocketServletWebServerCustomizer();
		}

	}

	@Controller
	static class MessagingController {

		@SubscribeMapping("/json")
		Data json() {
			return new Data(5, "baz");
		}

		@SubscribeMapping("/string")
		String string() {
			return "string data";
		}

	}

	public static class Data {

		private int foo;

		private String bar;

		Data(int foo, String bar) {
			this.foo = foo;
			this.bar = bar;
		}

		public int getFoo() {
			return this.foo;
		}

		public String getBar() {
			return this.bar;
		}

	}

}
