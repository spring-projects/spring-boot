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

package smoketest.websocket.jetty.client;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * SimpleClientWebSocketHandler class.
 */
public class SimpleClientWebSocketHandler extends TextWebSocketHandler {

	protected Log logger = LogFactory.getLog(SimpleClientWebSocketHandler.class);

	private final GreetingService greetingService;

	private final CountDownLatch latch;

	private final AtomicReference<String> messagePayload;

	/**
     * Constructs a new SimpleClientWebSocketHandler with the specified GreetingService, CountDownLatch, and AtomicReference.
     * 
     * @param greetingService the GreetingService to be used by the WebSocket handler
     * @param latch the CountDownLatch to be used for synchronization
     * @param message the AtomicReference to store the message payload
     */
    public SimpleClientWebSocketHandler(GreetingService greetingService, CountDownLatch latch,
			AtomicReference<String> message) {
		this.greetingService = greetingService;
		this.latch = latch;
		this.messagePayload = message;
	}

	/**
     * This method is called after a WebSocket connection is established.
     * It sends a greeting message to the client.
     *
     * @param session The WebSocket session that was established.
     * @throws Exception If an error occurs while sending the message.
     */
    @Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		TextMessage message = new TextMessage(this.greetingService.getGreeting());
		session.sendMessage(message);
	}

	/**
     * Handles a text message received from the WebSocket session.
     * 
     * @param session the WebSocket session
     * @param message the text message received
     * @throws Exception if an error occurs while handling the message
     */
    @Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		this.logger.info("Received: " + message + " (" + this.latch.getCount() + ")");
		session.close();
		this.messagePayload.set(message.getPayload());
		this.latch.countDown();
	}

}
