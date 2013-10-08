package org.springframework.boot.samples.websocket.snake;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class SnakeTimerTests {

    @Test
    public void removeDysfunctionalSnakes() throws Exception {
        Snake snake = mock(Snake.class);
        doThrow(new IOException()).when(snake).sendMessage(anyString());
        SnakeTimer.addSnake(snake);

        SnakeTimer.broadcast("");
        assertThat(SnakeTimer.getSnakes().size(), is(0));
    }
}
