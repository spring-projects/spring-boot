/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.websocket.tomcat.snake;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

/**
 * Sets up the timer for the multiplayer snake game WebSocket example.
 */
public final class SnakeTimer {

	private static final long TICK_DELAY = 100;

	private static final Object MONITOR = new Object();

	private static final Log logger = LogFactory.getLog(SnakeTimer.class);

	private static final ConcurrentHashMap<Integer, Snake> snakes = new ConcurrentHashMap<>();

	private static @Nullable Timer gameTimer;

	private SnakeTimer() {
	}

	public static void addSnake(Snake snake) {
		synchronized (MONITOR) {
			if (snakes.isEmpty()) {
				startTimer();
			}
			snakes.put(snake.getId(), snake);
		}
	}

	public static Collection<Snake> getSnakes() {
		return Collections.unmodifiableCollection(snakes.values());
	}

	public static void removeSnake(Snake snake) {
		synchronized (MONITOR) {
			snakes.remove(snake.getId());
			if (snakes.isEmpty()) {
				stopTimer();
			}
		}
	}

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

	public static void stopTimer() {
		if (gameTimer != null) {
			gameTimer.cancel();
		}
	}

}
