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

package samples.websocket.jetty.snake;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets up the timer for the multi-player snake game WebSocket example.
 */
public final class SnakeTimer {

	private static final long TICK_DELAY = 100;

	private static final Object MONITOR = new Object();

	private static final Logger log = LoggerFactory.getLogger(SnakeTimer.class);

	private static final ConcurrentHashMap<Integer, Snake> snakes = new ConcurrentHashMap<Integer, Snake>();

	private static Timer gameTimer = null;

	private SnakeTimer() {
	}

	public static void addSnake(Snake snake) {
		synchronized (MONITOR) {
			if (snakes.isEmpty()) {
				startTimer();
			}
			snakes.put(Integer.valueOf(snake.getId()), snake);
		}
	}

	public static Collection<Snake> getSnakes() {
		return Collections.unmodifiableCollection(snakes.values());
	}

	public static void removeSnake(Snake snake) {
		synchronized (MONITOR) {
			snakes.remove(Integer.valueOf(snake.getId()));
			if (snakes.isEmpty()) {
				stopTimer();
			}
		}
	}

	public static void tick() throws Exception {
		StringBuilder sb = new StringBuilder();
		for (Iterator<Snake> iterator = SnakeTimer.getSnakes().iterator(); iterator
				.hasNext();) {
			Snake snake = iterator.next();
			snake.update(SnakeTimer.getSnakes());
			sb.append(snake.getLocationsJson());
			if (iterator.hasNext()) {
				sb.append(',');
			}
		}
		broadcast(String.format("{'type': 'update', 'data' : [%s]}", sb.toString()));
	}

	public static void broadcast(String message) throws Exception {
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

	public static void startTimer() {
		gameTimer = new Timer(SnakeTimer.class.getSimpleName() + " Timer");
		gameTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					tick();
				}
				catch (Throwable ex) {
					log.error("Caught to prevent timer from shutting down", ex);
				}
			}
		}, TICK_DELAY, TICK_DELAY);
	}

	public static void stopTimer() {
		if (gameTimer != null) {
			gameTimer.cancel();
		}
	}

}
