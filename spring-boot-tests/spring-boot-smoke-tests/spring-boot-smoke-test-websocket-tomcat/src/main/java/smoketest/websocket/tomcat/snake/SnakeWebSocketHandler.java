/*
 * Copyright 2012-2024 the original author or authors.
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

package smoketest.websocket.tomcat.snake;

import java.awt.Color;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * SnakeWebSocketHandler class.
 */
public class SnakeWebSocketHandler extends TextWebSocketHandler {

	private static final AtomicInteger snakeIds = new AtomicInteger();

	private static final Random random = new Random();

	private final int id;

	private Snake snake;

	/**
	 * Generates a random hexadecimal color code.
	 * @return A string representing a random hexadecimal color code.
	 */
	public static String getRandomHexColor() {
		float hue = random.nextFloat();
		// sat between 0.1 and 0.3
		float saturation = (random.nextInt(2000) + 1000) / 10000f;
		float luminance = 0.9f;
		Color color = Color.getHSBColor(hue, saturation, luminance);
		return '#' + Integer.toHexString((color.getRGB() & 0xffffff) | 0x1000000).substring(1);
	}

	/**
	 * Generates a random location within the playfield.
	 * @return a Location object representing the random location
	 */
	public static Location getRandomLocation() {
		int x = roundByGridSize(random.nextInt(SnakeUtils.PLAYFIELD_WIDTH));
		int y = roundByGridSize(random.nextInt(SnakeUtils.PLAYFIELD_HEIGHT));
		return new Location(x, y);
	}

	/**
	 * Rounds the given value to the nearest multiple of the grid size.
	 * @param value the value to be rounded
	 * @return the rounded value
	 */
	private static int roundByGridSize(int value) {
		value = value + (SnakeUtils.GRID_SIZE / 2);
		value = value / SnakeUtils.GRID_SIZE;
		value = value * SnakeUtils.GRID_SIZE;
		return value;
	}

	/**
	 * Constructs a new SnakeWebSocketHandler object.
	 *
	 * This constructor initializes the id of the SnakeWebSocketHandler object by
	 * incrementing the snakeIds counter. The id is used to uniquely identify each
	 * SnakeWebSocketHandler object.
	 */
	public SnakeWebSocketHandler() {
		this.id = snakeIds.getAndIncrement();
	}

	/**
	 * This method is called after a WebSocket connection is established. It initializes a
	 * new Snake object with the given id and session, adds the snake to the SnakeTimer,
	 * and broadcasts a join message to all connected snakes.
	 * @param session The WebSocketSession object representing the connection.
	 * @throws Exception If an error occurs during the execution of the method.
	 */
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		this.snake = new Snake(this.id, session);
		SnakeTimer.addSnake(this.snake);
		StringBuilder sb = new StringBuilder();
		for (Iterator<Snake> iterator = SnakeTimer.getSnakes().iterator(); iterator.hasNext();) {
			Snake snake = iterator.next();
			sb.append(String.format("{id: %d, color: '%s'}", snake.getId(), snake.getHexColor()));
			if (iterator.hasNext()) {
				sb.append(',');
			}
		}
		SnakeTimer.broadcast(String.format("{'type': 'join','data':[%s]}", sb));
	}

	/**
	 * Handles a text message received from a WebSocket session.
	 * @param session the WebSocket session
	 * @param message the text message received
	 * @throws Exception if an error occurs while handling the message
	 */
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		String payload = message.getPayload();
		switch (payload) {
			case "west" -> this.snake.setDirection(Direction.WEST);
			case "north" -> this.snake.setDirection(Direction.NORTH);
			case "east" -> this.snake.setDirection(Direction.EAST);
			case "south" -> this.snake.setDirection(Direction.SOUTH);
		}
	}

	/**
	 * This method is called after a WebSocket connection is closed. It removes the snake
	 * from the SnakeTimer and broadcasts a leave message to all connected clients.
	 * @param session The WebSocketSession object representing the closed connection.
	 * @param status The CloseStatus object representing the reason for the connection
	 * closure.
	 * @throws Exception if an error occurs while removing the snake or broadcasting the
	 * leave message.
	 */
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		SnakeTimer.removeSnake(this.snake);
		SnakeTimer.broadcast(String.format("{'type': 'leave', 'id': %d}", this.id));
	}

}
