/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.metrics.statsd;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.util.SocketUtils;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link StatsdMetricWriter}.
 *
 * @author Dave Syer
 */
public class StatsdMetricWriterTests {

	private int port = SocketUtils.findAvailableTcpPort();

	private DummyStatsDServer server = new DummyStatsDServer(this.port);

	private StatsdMetricWriter writer = new StatsdMetricWriter("me", "localhost",
			this.port);

	@After
	public void close() {
		this.server.stop();
		this.writer.close();
	}

	@Test
	public void increment() {
		this.writer.increment(new Delta<Long>("counter.foo", 3L));
		this.server.waitForMessage();
		assertEquals("me.counter.foo:3|c", this.server.messagesReceived().get(0));
	}

	@Test
	public void setLongMetric() throws Exception {
		this.writer.set(new Metric<Long>("gauge.foo", 3L));
		this.server.waitForMessage();
		assertEquals("me.gauge.foo:3|g", this.server.messagesReceived().get(0));
	}

	@Test
	public void setDoubleMetric() throws Exception {
		this.writer.set(new Metric<Double>("gauge.foo", 3.7));
		this.server.waitForMessage();
		// Doubles are truncated
		assertEquals("me.gauge.foo:3.7|g", this.server.messagesReceived().get(0));
	}

	@Test
	public void setTimerMetric() throws Exception {
		this.writer.set(new Metric<Long>("timer.foo", 37L));
		this.server.waitForMessage();
		assertEquals("me.timer.foo:37|ms", this.server.messagesReceived().get(0));
	}

	@Test
	public void nullPrefix() throws Exception {
		this.writer = new StatsdMetricWriter("localhost", this.port);
		this.writer.set(new Metric<Long>("gauge.foo", 3L));
		this.server.waitForMessage();
		assertEquals("gauge.foo:3|g", this.server.messagesReceived().get(0));
	}

	@Test
	public void perioPrefix() throws Exception {
		this.writer = new StatsdMetricWriter("my.", "localhost", this.port);
		this.writer.set(new Metric<Long>("gauge.foo", 3L));
		this.server.waitForMessage();
		assertEquals("my.gauge.foo:3|g", this.server.messagesReceived().get(0));
	}

	private static final class DummyStatsDServer {

		private final List<String> messagesReceived = new ArrayList<String>();

		private final DatagramSocket server;

		DummyStatsDServer(int port) {
			try {
				this.server = new DatagramSocket(port);
			}
			catch (SocketException e) {
				throw new IllegalStateException(e);
			}
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						final DatagramPacket packet = new DatagramPacket(new byte[256],
								256);
						DummyStatsDServer.this.server.receive(packet);
						DummyStatsDServer.this.messagesReceived.add(
								new String(packet.getData(), Charset.forName("UTF-8"))
										.trim());
					}
					catch (Exception e) {
						// Ignore
					}
				}
			}).start();
		}

		public void stop() {
			this.server.close();
		}

		public void waitForMessage() {
			while (this.messagesReceived.isEmpty()) {
				try {
					Thread.sleep(50L);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}

		public List<String> messagesReceived() {
			return new ArrayList<String>(this.messagesReceived);
		}

	}

}
