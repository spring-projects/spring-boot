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

import java.net.URI;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.StandardWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SpringBootWebsocketReactiveSampleApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@LocalServerPort
	private String port;

	@Test
	public void contextLoads() {
	}

	@Test
	public void testIfSamplePageLoads() throws Exception {
		WebTestClient webTestClient = WebTestClient
				.bindToApplicationContext(this.applicationContext).build();
		webTestClient.get().uri("/").exchange().expectBody(String.class)
				.consumeWith((stringEntityExchangeResult) -> {
					Assertions.assertThat(stringEntityExchangeResult.getResponseBody())
							.isNotEmpty().contains("Spring Boot Sample - Websocket");
				});

	}

	@Test
	public void testIfWebSocketWorks() {
		URI serverUri = URI.create(String.format("ws://localhost:%s/ws/echo", this.port));
		WebSocketClient webSocketClient = new StandardWebSocketClient();
		webSocketClient.execute(serverUri, (webSocketSession) -> {
			String msg = "My Message";
			Flux<String> result = webSocketSession.receive()
					.map(WebSocketMessage::getPayloadAsText);
			webSocketSession.send(Mono.just(msg).map(webSocketSession::textMessage));

			result.subscribe(new Subscriber<String>() {
				private Subscription subscription = null;

				private String lastMessage = null;

				@Override
				public void onSubscribe(Subscription subscription) {
					this.subscription = subscription;
				}

				@Override
				public void onNext(String s) {
					this.lastMessage = s;
				}

				@Override
				public void onError(Throwable throwable) {

				}

				@Override
				public void onComplete() {
					Assertions.assertThat(this.lastMessage).endsWith(msg);
				}
			});

			return Mono.empty();
		});

	}

}
