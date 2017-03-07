/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.metrics.graphite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GraphiteMetricWriter}.
 *
 * @author Mark Sailes
 */
public class GraphiteMetricWriterTest {

	private int port = SocketUtils.findAvailableTcpPort();

	private DummyGraphiteServer server = new DummyGraphiteServer(this.port);

	private GraphiteMetricWriter writer = new GraphiteMetricWriter("me", "localhost",
			this.port);

	@After
	public void close() {
		this.server.stop();
	}

	@Test
	public void increment() {
		this.writer.increment(new Delta<Long>("counter.foo", 3L, new Date(1488405805)));
		this.server.waitForMessage();
		assertThat(this.server.messagesReceived().get(0))
				.isEqualTo("me.counter.foo 3 1488405");
	}

	@Test
	public void setLongMetric() throws Exception {
		this.writer.set(new Metric<Long>("gauge.foo", 3L, new Date(1488405805)));
		this.server.waitForMessage();
		assertThat(this.server.messagesReceived().get(0))
				.isEqualTo("me.gauge.foo 3 1488405");
	}

	@Test
	public void setDoubleMetric() throws Exception {
		this.writer.set(new Metric<Double>("gauge.foo", 3.7, new Date(1488405805)));
		this.server.waitForMessage();
		assertThat(this.server.messagesReceived().get(0))
				.isEqualTo("me.gauge.foo 3.7 1488405");
	}

	@Test
	public void setTimerMetric() throws Exception {
		this.writer.set(new Metric<Long>("timer.foo", 37L, new Date(1488405805)));
		this.server.waitForMessage();
		assertThat(this.server.messagesReceived().get(0))
				.isEqualTo("me.timer.foo 37 1488405");
	}

	@Test
	public void nullPrefix() throws Exception {
		this.writer = new GraphiteMetricWriter("localhost", this.port);
		this.writer.set(new Metric<Long>("gauge.foo", 3L, new Date(1488405805)));
		this.server.waitForMessage();
		assertThat(this.server.messagesReceived().get(0))
				.isEqualTo("gauge.foo 3 1488405");
	}

	@Test
	public void periodPrefix() throws Exception {
		this.writer = new GraphiteMetricWriter("my.", "localhost", this.port);
		this.writer.set(new Metric<Long>("gauge.foo", 3L, new Date(1488405805)));
		this.server.waitForMessage();
		assertThat(this.server.messagesReceived().get(0))
				.isEqualTo("my.gauge.foo 3 1488405");
	}

	private static final class DummyGraphiteServer implements Runnable {

		private final List<String> messagesReceived = new ArrayList<>();

		private final ServerSocket server;

		DummyGraphiteServer(int port) {
			try {
				this.server = new ServerSocket(port);
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
			new Thread(this).start();
		}

		void stop() {
			try {
				this.server.close();
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public void run() {
			try (Socket socket = this.server.accept();
					BufferedReader in = new BufferedReader(
							new InputStreamReader(socket.getInputStream()))) {

				String serverResponse;
				while ((serverResponse = in.readLine()) != null) {
					this.messagesReceived.add(serverResponse);
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		void waitForMessage() {
			while (this.messagesReceived.isEmpty()) {
				try {
					Thread.sleep(50L);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}

		List<String> messagesReceived() {
			return new ArrayList<>(this.messagesReceived);
		}

	}
}
