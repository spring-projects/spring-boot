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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Snake class.
 */
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

	/**
     * Constructs a new Snake object with the specified id and WebSocketSession.
     * 
     * @param id the id of the snake
     * @param session the WebSocketSession associated with the snake
     */
    public Snake(int id, WebSocketSession session) {
		this.id = id;
		this.session = session;
		this.hexColor = SnakeUtils.getRandomHexColor();
		resetState();
	}

	/**
     * Resets the state of the snake.
     * This method sets the direction of the snake to NONE,
     * generates a random location for the snake's head,
     * clears the tail of the snake,
     * and sets the length of the snake to the default length.
     */
    private void resetState() {
		this.direction = Direction.NONE;
		this.head = SnakeUtils.getRandomLocation();
		this.tail.clear();
		this.length = DEFAULT_LENGTH;
	}

	/**
     * Kills the snake and updates its state.
     * 
     * @throws Exception if an error occurs during the process
     */
    private void kill() throws Exception {
		synchronized (this.monitor) {
			resetState();
			sendMessage("{'type': 'dead'}");
		}
	}

	/**
     * Increases the length of the snake and sends a message to kill it.
     *
     * @throws Exception if an error occurs during the execution of the method.
     */
    private void reward() throws Exception {
		synchronized (this.monitor) {
			this.length++;
			sendMessage("{'type': 'kill'}");
		}
	}

	/**
     * Sends a message using the current session.
     * 
     * @param msg the message to be sent
     * @throws Exception if an error occurs while sending the message
     */
    protected void sendMessage(String msg) throws Exception {
		this.session.sendMessage(new TextMessage(msg));
	}

	/**
     * Updates the snake's position based on its current direction.
     * If the snake reaches the edge of the playfield, it wraps around to the opposite side.
     * The snake's tail is updated accordingly, removing the last segment if necessary.
     * 
     * @param snakes the collection of other snakes in the game
     * @throws Exception if an error occurs during the update process
     */
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

	/**
     * Handles collisions between the current snake and a collection of other snakes.
     * If a collision occurs, the current snake is killed and the other snake is rewarded.
     * 
     * @param snakes the collection of other snakes to check for collisions with
     * @throws Exception if an error occurs during collision handling
     */
    private void handleCollisions(Collection<Snake> snakes) throws Exception {
		for (Snake snake : snakes) {
			boolean headCollision = this.id != snake.id && snake.getHead().equals(this.head);
			boolean tailCollision = snake.getTail().contains(this.head);
			if (headCollision || tailCollision) {
				kill();
				if (this.id != snake.id) {
					snake.reward();
				}
			}
		}
	}

	/**
     * Returns the current location of the head of the snake.
     *
     * @return the location of the head of the snake
     */
    public Location getHead() {
		synchronized (this.monitor) {
			return this.head;
		}
	}

	/**
     * Returns the tail of the snake.
     *
     * @return the tail of the snake as a Collection of Location objects.
     */
    public Collection<Location> getTail() {
		synchronized (this.monitor) {
			return this.tail;
		}
	}

	/**
     * Sets the direction of the snake.
     * 
     * @param direction the new direction of the snake
     */
    public void setDirection(Direction direction) {
		synchronized (this.monitor) {
			this.direction = direction;
		}
	}

	/**
     * Returns the JSON representation of the locations of the snake.
     *
     * @return the JSON string representing the locations of the snake
     */
    public String getLocationsJson() {
		synchronized (this.monitor) {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("{x: %d, y: %d}", this.head.x, this.head.y));
			for (Location location : this.tail) {
				sb.append(',');
				sb.append(String.format("{x: %d, y: %d}", location.x, location.y));
			}
			return String.format("{'id':%d,'body':[%s]}", this.id, sb);
		}
	}

	/**
     * Returns the ID of the snake.
     *
     * @return the ID of the snake
     */
    public int getId() {
		return this.id;
	}

	/**
     * Returns the hexadecimal color code of the snake.
     *
     * @return the hexadecimal color code of the snake
     */
    public String getHexColor() {
		return this.hexColor;
	}

}
