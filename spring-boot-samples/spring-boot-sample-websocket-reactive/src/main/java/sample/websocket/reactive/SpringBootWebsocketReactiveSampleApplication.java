/*
 * Copyright 2012-2017 the original author or authors.
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

package sample.websocket.reactive;

import java.util.HashMap;
import java.util.Map;

import sample.websocket.reactive.websockets.SampleEchoWebSocketHandler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

@SpringBootApplication
public class SpringBootWebsocketReactiveSampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootWebsocketReactiveSampleApplication.class, args);
	}

	@Bean
	public WebSocketHandlerAdapter handlerAdapter() {
		WebSocketHandlerAdapter webSocketHandlerAdapter = new WebSocketHandlerAdapter();
		return webSocketHandlerAdapter;
	}

	@Bean
	public HandlerMapping webSockingHandlerMapping() {
		Map<String, WebSocketHandler> handlerMap = new HashMap<>();
		handlerMap.put("/ws/echo", new SampleEchoWebSocketHandler());

		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setOrder(1);
		handlerMapping.setUrlMap(handlerMap);
		return handlerMapping;
	}

}
