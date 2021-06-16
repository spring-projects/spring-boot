/*
 * Copyright 2012-2021 the original author or authors.
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

package smoketest.websocket.jetty10;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import smoketest.websocket.jetty10.client.GreetingService;
import smoketest.websocket.jetty10.client.SimpleClientWebSocketHandler;
import smoketest.websocket.jetty10.client.SimpleGreetingService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledForJreRange(min = JRE.JAVA_11)
@SpringBootTest(classes = SampleJetty10WebSocketsApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "debug: true")
class SampleJetty10WebSocketsApplicationTests {

	private static Log logger = LogFactory.getLog(SampleJetty10WebSocketsApplicationTests.class);

	@LocalServerPort
	private int port = 1234;

	@Test
	void echoEndpoint() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(ClientConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class)
						.properties("websocket.uri:ws://localhost:" + this.port + "/echo/websocket")
						.run("--spring.main.web-application-type=none");
		long count = context.getBean(ClientConfiguration.class).latch.getCount();
		AtomicReference<String> messagePayloadReference = context.getBean(ClientConfiguration.class).messagePayload;
		context.close();
		assertThat(count).isEqualTo(0);
		assertThat(messagePayloadReference.get()).isEqualTo("Did you say \"Hello world!\"?");
	}

	@Test
	void reverseEndpoint() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(ClientConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class)
						.properties("websocket.uri:ws://localhost:" + this.port + "/reverse")
						.run("--spring.main.web-application-type=none");
		long count = context.getBean(ClientConfiguration.class).latch.getCount();
		AtomicReference<String> messagePayloadReference = context.getBean(ClientConfiguration.class).messagePayload;
		context.close();
		assertThat(count).isEqualTo(0);
		assertThat(messagePayloadReference.get()).isEqualTo("Reversed: !dlrow olleH");
	}

	@Configuration(proxyBeanMethods = false)
	static class ClientConfiguration implements CommandLineRunner {

		@Value("${websocket.uri}")
		private String webSocketUri;

		private final CountDownLatch latch = new CountDownLatch(1);

		private final AtomicReference<String> messagePayload = new AtomicReference<>();

		@Override
		public void run(String... args) throws Exception {
			logger.info("Waiting for response: latch=" + this.latch.getCount());
			if (this.latch.await(10, TimeUnit.SECONDS)) {
				logger.info("Got response: " + this.messagePayload.get());
			}
			else {
				logger.info("Response not received: latch=" + this.latch.getCount());
			}
		}

		@Bean
		WebSocketConnectionManager wsConnectionManager() {
			WebSocketConnectionManager manager = new WebSocketConnectionManager(client(), handler(), this.webSocketUri);
			manager.setAutoStartup(true);
			return manager;
		}

		@Bean
		StandardWebSocketClient client() {
			return new StandardWebSocketClient();
		}

		@Bean
		SimpleClientWebSocketHandler handler() {
			return new SimpleClientWebSocketHandler(greetingService(), this.latch, this.messagePayload);
		}

		@Bean
		GreetingService greetingService() {
			return new SimpleGreetingService();
		}

	}

}
