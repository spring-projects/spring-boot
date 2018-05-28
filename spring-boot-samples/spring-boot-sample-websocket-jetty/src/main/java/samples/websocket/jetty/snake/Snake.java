/*
 * Copyright 2012-2018 the original author or authors.
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

package samples.websocket.jetty.snake;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class Snake {

	private static final int DEFAULT_LENGTH = 5;

	private final Deque<Location> tail = new ArrayDeque<>();

	private final Object monitor = new Object();

	private final int id;

	private final WebSocketSession session;

	private final String hexColor;

	private Direction direction;

	private int length = DEFAULT_LENGTH;

	private Location head;

	public Snake(int id, WebSocketSession session) {
		this.id = id;
		this.session = session;
		this.hexColor = SnakeUtils.getRandomHexColor();
		resetState();
	}

	private void resetState() {
		this.direction = Direction.NONE;
		this.head = SnakeUtils.getRandomLocation();
		this.tail.clear();
		this.length = DEFAULT_LENGTH;
	}

	private void kill() throws Exception {
		synchronized (this.monitor) {
			resetState();
			sendMessage("{'type': 'dead'}");
		}
	}

	private void reward() throws Exception {
		synchronized (this.monitor) {
			this.length++;
			sendMessage("{'type': 'kill'}");
		}
	}

	protected void sendMessage(String msg) throws Exception {
		this.session.sendMessage(new TextMessage(msg));
	}

	public void update(Collection<Snake> snakes) throws Exception {
		synchronized (this.monitor) {
			Location nextLocation = this.head.getAdjacentLocation(this.direction);
			if (nextLocation.x >= SnakeUtils.PLAYFIELD_WIDTH) {
				nextLocation.x = 0;
			}
			if (nextLocation.y >= SnakeUtils.PLAYFIELD_HEIGHT) {
				nextLocation.y = 0;
			}
			if (nextLocation.x < 0) {
				nextLocation.x = SnakeUtils.PLAYFIELD_WIDTH;
			}
			if (nextLocation.y < 0) {
				nextLocation.y = SnakeUtils.PLAYFIELD_HEIGHT;
			}
			if (this.direction != Direction.NONE) {
				this.tail.addFirst(this.head);
				if (this.tail.size() > this.length) {
					this.tail.removeLast();
				}
				this.head = nextLocation;
			}

			handleCollisions(snakes);
		}
	}

	private void handleCollisions(Collection<Snake> snakes) throws Exception {
		for (Snake snake : snakes) {
			boolean headCollision = this.id != snake.id
					&& snake.getHead().equals(this.head);
			boolean tailCollision = snake.getTail().contains(this.head);
			if (headCollision || tailCollision) {
				kill();
				if (this.id != snake.id) {
					snake.reward();
				}
			}
		}
	}

	public Location getHead() {
		synchronized (this.monitor) {
			return this.head;
		}
	}

	public Collection<Location> getTail() {
		synchronized (this.monitor) {
			return this.tail;
		}
	}

	public void setDirection(Direction direction) {
		synchronized (this.monitor) {
			this.direction = direction;
		}
	}

	public String getLocationsJson() {
		synchronized (this.monitor) {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("{x: %d, y: %d}", Integer.valueOf(this.head.x),
					Integer.valueOf(this.head.y)));
			for (Location location : this.tail) {
				sb.append(',');
				sb.append(String.format("{x: %d, y: %d}", Integer.valueOf(location.x),
						Integer.valueOf(location.y)));
			}
			return String.format("{'id':%d,'body':[%s]}", Integer.valueOf(this.id),
					sb.toString());
		}
	}

	public int getId() {
		return this.id;
	}

	public String getHexColor() {
		return this.hexColor;
	}

}
