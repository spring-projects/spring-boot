/*
 * Copyright 2012-2018 the original author or authors.
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

package samples.websocket.tomcat.snake;

import java.awt.Color;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class SnakeWebSocketHandler extends TextWebSocketHandler {

	private static final AtomicInteger snakeIds = new AtomicInteger(0);

	private static final Random random = new Random();

	private final int id;

	private Snake snake;

	public static String getRandomHexColor() {
		float hue = random.nextFloat();
		// sat between 0.1 and 0.3
		float saturation = (random.nextInt(2000) + 1000) / 10000f;
		float luminance = 0.9f;
		Color color = Color.getHSBColor(hue, saturation, luminance);
		return '#' + Integer.toHexString((color.getRGB() & 0xffffff) | 0x1000000)
				.substring(1);
	}

	public static Location getRandomLocation() {
		int x = roundByGridSize(random.nextInt(SnakeUtils.PLAYFIELD_WIDTH));
		int y = roundByGridSize(random.nextInt(SnakeUtils.PLAYFIELD_HEIGHT));
		return new Location(x, y);
	}

	private static int roundByGridSize(int value) {
		value = value + (SnakeUtils.GRID_SIZE / 2);
		value = value / SnakeUtils.GRID_SIZE;
		value = value * SnakeUtils.GRID_SIZE;
		return value;
	}

	public SnakeWebSocketHandler() {
		this.id = snakeIds.getAndIncrement();
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		this.snake = new Snake(this.id, session);
		SnakeTimer.addSnake(this.snake);
		StringBuilder sb = new StringBuilder();
		for (Iterator<Snake> iterator = SnakeTimer.getSnakes().iterator(); iterator
				.hasNext();) {
			Snake snake = iterator.next();
			sb.append(String.format("{id: %d, color: '%s'}",
					Integer.valueOf(snake.getId()), snake.getHexColor()));
			if (iterator.hasNext()) {
				sb.append(',');
			}
		}
		SnakeTimer
				.broadcast(String.format("{'type': 'join','data':[%s]}", sb.toString()));
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message)
			throws Exception {
		String payload = message.getPayload();
		if ("west".equals(payload)) {
			this.snake.setDirection(Direction.WEST);
		}
		else if ("north".equals(payload)) {
			this.snake.setDirection(Direction.NORTH);
		}
		else if ("east".equals(payload)) {
			this.snake.setDirection(Direction.EAST);
		}
		else if ("south".equals(payload)) {
			this.snake.setDirection(Direction.SOUTH);
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
			throws Exception {
		SnakeTimer.removeSnake(this.snake);
		SnakeTimer.broadcast(
				String.format("{'type': 'leave', 'id': %d}", Integer.valueOf(this.id)));
	}

}
