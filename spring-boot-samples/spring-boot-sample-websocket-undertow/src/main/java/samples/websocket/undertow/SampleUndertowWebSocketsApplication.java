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

package samples.websocket.undertow;

import samples.websocket.undertow.client.GreetingService;
import samples.websocket.undertow.client.SimpleGreetingService;
import samples.websocket.undertow.echo.DefaultEchoService;
import samples.websocket.undertow.echo.EchoService;
import samples.websocket.undertow.echo.EchoWebSocketHandler;
import samples.websocket.undertow.reverse.ReverseWebSocketEndpoint;
import samples.websocket.undertow.snake.SnakeWebSocketHandler;

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

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EnableWebSocket
public class SampleUndertowWebSocketsApplication extends SpringBootServletInitializer
		implements WebSocketConfigurer {

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(echoWebSocketHandler(), "/echo").setAllowedOrigins("*")
				.withSockJS();
		registry.addHandler(snakeWebSocketHandler(), "/snake").setAllowedOrigins("*")
				.withSockJS();
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(SampleUndertowWebSocketsApplication.class);
	}

	@Bean
	public EchoService echoService() {
		return new DefaultEchoService("Did you say \"%s\"?");
	}

	@Bean
	public GreetingService greetingService() {
		return new SimpleGreetingService();
	}

	@Bean
	public WebSocketHandler echoWebSocketHandler() {
		return new EchoWebSocketHandler(echoService());
	}

	@Bean
	public WebSocketHandler snakeWebSocketHandler() {
		return new PerConnectionWebSocketHandler(SnakeWebSocketHandler.class);
	}

	@Bean
	public ReverseWebSocketEndpoint reverseWebSocketEndpoint() {
		return new ReverseWebSocketEndpoint();
	}

	@Bean
	public ServerEndpointExporter serverEndpointExporter() {
		return new ServerEndpointExporter();
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleUndertowWebSocketsApplication.class, args);
	}

}
