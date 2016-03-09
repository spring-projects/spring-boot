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

package org.springframework.boot.actuate.metrics.writer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link MessageChannelMetricWriter} and {@link MetricWriterMessageHandler}.
 *
 * @author Dave Syer
 */
public class MessageChannelMetricWriterTests {

	@Mock
	private MessageChannel channel;

	@Mock
	private MetricWriter observer;

	private MessageChannelMetricWriter writer;

	private MetricWriterMessageHandler handler;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		given(this.channel.send(any(Message.class))).willAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				MessageChannelMetricWriterTests.this.handler
						.handleMessage(invocation.getArgumentAt(0, Message.class));
				return true;
			}

		});
		this.writer = new MessageChannelMetricWriter(this.channel);
		this.handler = new MetricWriterMessageHandler(this.observer);
	}

	@Test
	public void messageSentOnAdd() {
		this.writer.increment(new Delta<Integer>("foo", 1));
		verify(this.channel).send(any(Message.class));
		verify(this.observer).increment(any(Delta.class));
	}

	@Test
	public void messageSentOnSet() {
		this.writer.set(new Metric<Double>("foo", 1d));
		verify(this.channel).send(any(Message.class));
		verify(this.observer).set(any(Metric.class));
	}

	@Test
	public void messageSentOnReset() throws Exception {
		this.writer.reset("foo");
		verify(this.channel).send(any(Message.class));
		verify(this.observer).reset("foo");
	}

}
