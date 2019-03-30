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

import java.io.IOException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

public class SnakeTimerTests {

	@Test
	public void removeDysfunctionalSnakes() throws Exception {
		Snake snake = mock(Snake.class);
		willThrow(new IOException()).given(snake).sendMessage(anyString());
		SnakeTimer.addSnake(snake);

		SnakeTimer.broadcast("");
		assertThat(SnakeTimer.getSnakes()).hasSize(0);
	}

}
