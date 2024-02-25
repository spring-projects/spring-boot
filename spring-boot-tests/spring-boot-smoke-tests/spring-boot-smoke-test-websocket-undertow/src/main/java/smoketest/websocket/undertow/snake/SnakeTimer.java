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

package smoketest.websocket.undertow.snake;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Sets up the timer for the multiplayer snake game WebSocket example.
 */
public final class SnakeTimer {

	private static final long TICK_DELAY = 100;

	private static final Object MONITOR = new Object();

	private static final Log logger = LogFactory.getLog(SnakeTimer.class);

	private static final ConcurrentHashMap<Integer, Snake> snakes = new ConcurrentHashMap<>();

	private static Timer gameTimer = null;

	/**
     * Constructs a new SnakeTimer object.
     */
    private SnakeTimer() {
	}

	/**
     * Adds a snake to the collection of snakes and starts the timer if it is the first snake added.
     *
     * @param snake the snake to be added
     */
    public static void addSnake(Snake snake) {
		synchronized (MONITOR) {
			if (snakes.isEmpty()) {
				startTimer();
			}
			snakes.put(snake.getId(), snake);
		}
	}

	/**
     * Returns an unmodifiable collection of all the snakes in the SnakeTimer.
     *
     * @return an unmodifiable collection of snakes
     */
    public static Collection<Snake> getSnakes() {
		return Collections.unmodifiableCollection(snakes.values());
	}

	/**
     * Removes the specified snake from the list of snakes and stops the timer if there are no more snakes.
     *
     * @param snake the snake to be removed
     */
    public static void removeSnake(Snake snake) {
		synchronized (MONITOR) {
			snakes.remove(snake.getId());
			if (snakes.isEmpty()) {
				stopTimer();
			}
		}
	}

	/**
     * Updates the snakes and broadcasts the updated locations in JSON format.
     * 
     * @throws Exception if an error occurs during the update process
     */
    public static void tick() throws Exception {
		StringBuilder sb = new StringBuilder();
		for (Iterator<Snake> iterator = SnakeTimer.getSnakes().iterator(); iterator.hasNext();) {
			Snake snake = iterator.next();
			snake.update(SnakeTimer.getSnakes());
			sb.append(snake.getLocationsJson());
			if (iterator.hasNext()) {
				sb.append(',');
			}
		}
		broadcast(String.format("{'type': 'update', 'data' : [%s]}", sb));
	}

	/**
     * Broadcasts a message to all connected snakes.
     * 
     * @param message the message to be broadcasted
     */
    public static void broadcast(String message) {
		Collection<Snake> snakes = new CopyOnWriteArrayList<>(SnakeTimer.getSnakes());
		for (Snake snake : snakes) {
			try {
				snake.sendMessage(message);
			}
			catch (Throwable ex) {
				// if Snake#sendMessage fails the client is removed
				removeSnake(snake);
			}
		}
	}

	/**
     * Starts the game timer.
     * 
     * The game timer is responsible for scheduling and executing the tick() method at a fixed rate.
     * 
     * @throws Throwable if an error occurs while executing the tick() method
     */
    public static void startTimer() {
		gameTimer = new Timer(SnakeTimer.class.getSimpleName() + " Timer");
		gameTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					tick();
				}
				catch (Throwable ex) {
					logger.error("Caught to prevent timer from shutting down", ex);
				}
			}
		}, TICK_DELAY, TICK_DELAY);
	}

	/**
     * Stops the game timer if it is running.
     */
    public static void stopTimer() {
		if (gameTimer != null) {
			gameTimer.cancel();
		}
	}

}
