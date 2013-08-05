/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.samples.websocket.echo;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.samples.websocket.client.GreetingService;
import org.springframework.boot.samples.websocket.client.SimpleClientWebSocketHandler;
import org.springframework.boot.samples.websocket.client.SimpleGreetingService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.endpoint.StandardWebSocketClient;

public class StandardClientApp {

	private static Log logger = LogFactory.getLog(StandardClientApp.class);

	private static final String WS_URI = "ws://localhost:8080/echo";

	@Test
	public void runAndWait() throws Exception {
		ApplicationContext context = SpringApplication.run(ClientConfiguration.class, "--spring.main.web_environment=false");
		assertEquals(0, context.getBean(ClientConfiguration.class).latch.getCount());
	}

	@Configuration
	static class ClientConfiguration implements CommandLineRunner {

		private CountDownLatch latch = new CountDownLatch(1);

		@Override
		public void run(String... args) throws Exception {
			logger.info("Waiting for response: latch=" + latch.getCount());
			latch.await();
			logger.info("Got response: latch=" + latch.getCount());
		}

		@Bean
		public WebSocketConnectionManager wsConnectionManager() {

			WebSocketConnectionManager manager = new WebSocketConnectionManager(client(), handler(), WS_URI);
			manager.setAutoStartup(true);

			return manager;
		}

		@Bean
		public StandardWebSocketClient client() {
			return new StandardWebSocketClient();
		}

		@Bean
		public SimpleClientWebSocketHandler handler() {
			return new SimpleClientWebSocketHandler(greetingService(), latch);
		}

		@Bean
		public GreetingService greetingService() {
			return new SimpleGreetingService();
		}
	}

}
