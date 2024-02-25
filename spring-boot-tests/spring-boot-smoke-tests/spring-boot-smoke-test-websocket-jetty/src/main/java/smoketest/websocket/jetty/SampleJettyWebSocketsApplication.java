/*
 * Copyright 2012-2019 the original author or authors.
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

package smoketest.websocket.jetty;

import smoketest.websocket.jetty.client.GreetingService;
import smoketest.websocket.jetty.client.SimpleGreetingService;
import smoketest.websocket.jetty.echo.DefaultEchoService;
import smoketest.websocket.jetty.echo.EchoService;
import smoketest.websocket.jetty.echo.EchoWebSocketHandler;
import smoketest.websocket.jetty.reverse.ReverseWebSocketEndpoint;
import smoketest.websocket.jetty.snake.SnakeWebSocketHandler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * SampleJettyWebSocketsApplication class.
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EnableWebSocket
public class SampleJettyWebSocketsApplication extends SpringBootServletInitializer implements WebSocketConfigurer {

	/**
	 * Registers WebSocket handlers for the application.
	 * @param registry the WebSocketHandlerRegistry to register the handlers with
	 */
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(echoWebSocketHandler(), "/echo").withSockJS();
		registry.addHandler(snakeWebSocketHandler(), "/snake").withSockJS();
	}

	/**
	 * Configures the Spring application builder.
	 * @param application the Spring application builder
	 * @return the sources of the Spring application
	 */
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(SampleJettyWebSocketsApplication.class);
	}

	/**
	 * Creates and returns an instance of the EchoService interface.
	 * @return the EchoService instance
	 */
	@Bean
	public EchoService echoService() {
		return new DefaultEchoService("Did you say \"%s\"?");
	}

	/**
	 * Creates a new instance of the GreetingService interface using the
	 * SimpleGreetingService implementation.
	 * @return the newly created GreetingService instance
	 */
	@Bean
	public GreetingService greetingService() {
		return new SimpleGreetingService();
	}

	/**
	 * Creates a WebSocketHandler that handles incoming WebSocket messages by echoing them
	 * back to the sender.
	 * @return the created WebSocketHandler
	 */
	@Bean
	public WebSocketHandler echoWebSocketHandler() {
		return new EchoWebSocketHandler(echoService());
	}

	/**
	 * Creates a WebSocketHandler for handling snake game WebSocket connections.
	 * @return the WebSocketHandler instance for snake game WebSocket connections
	 */
	@Bean
	public WebSocketHandler snakeWebSocketHandler() {
		return new PerConnectionWebSocketHandler(SnakeWebSocketHandler.class);
	}

	/**
	 * Creates a new instance of ReverseWebSocketEndpoint.
	 * @return the newly created ReverseWebSocketEndpoint instance
	 */
	@Bean
	public ReverseWebSocketEndpoint reverseWebSocketEndpoint() {
		return new ReverseWebSocketEndpoint();
	}

	/**
	 * Initializes and configures the ServerEndpointExporter.
	 * @return the ServerEndpointExporter instance
	 */
	@Bean
	public ServerEndpointExporter serverEndpointExporter() {
		return new ServerEndpointExporter();
	}

	/**
	 * The main method is the entry point of the application. It starts the Spring
	 * application by running the SampleJettyWebSocketsApplication class.
	 * @param args the command line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(SampleJettyWebSocketsApplication.class, args);
	}

}
