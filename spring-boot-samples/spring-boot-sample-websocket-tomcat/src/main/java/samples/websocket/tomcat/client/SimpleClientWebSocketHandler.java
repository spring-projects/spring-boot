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

package samples.websocket.tomcat.client;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class SimpleClientWebSocketHandler extends TextWebSocketHandler {

	protected Log logger = LogFactory.getLog(SimpleClientWebSocketHandler.class);

	private final GreetingService greetingService;

	private final CountDownLatch latch;

	private final AtomicReference<String> messagePayload;

	public SimpleClientWebSocketHandler(GreetingService greetingService, CountDownLatch latch,
			AtomicReference<String> message) {
		this.greetingService = greetingService;
		this.latch = latch;
		this.messagePayload = message;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		TextMessage message = new TextMessage(this.greetingService.getGreeting());
		session.sendMessage(message);
	}

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		this.logger.info("Received: " + message + " (" + this.latch.getCount() + ")");
		session.close();
		this.messagePayload.set(message.getPayload());
		this.latch.countDown();
	}

}
