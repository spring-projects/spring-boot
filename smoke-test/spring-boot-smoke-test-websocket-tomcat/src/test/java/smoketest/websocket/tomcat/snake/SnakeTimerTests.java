/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.websocket.tomcat.snake;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

class SnakeTimerTests {

	@Test
	void removeDysfunctionalSnakes() throws Exception {
		Snake snake = mock(Snake.class);
		willThrow(new IOException()).given(snake).sendMessage(anyString());
		SnakeTimer.addSnake(snake);
		SnakeTimer.broadcast("");
		assertThat(SnakeTimer.getSnakes()).isEmpty();
	}

}
