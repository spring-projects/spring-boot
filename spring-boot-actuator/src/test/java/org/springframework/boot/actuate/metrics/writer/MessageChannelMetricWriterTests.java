/*
 * Copyright 2012-2013 the original author or authors.
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

import org.junit.Test;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Dave Syer
 */
public class MessageChannelMetricWriterTests {

	private MessageChannel channel = mock(MessageChannel.class);

	private MessageChannelMetricWriter observer = new MessageChannelMetricWriter(
			this.channel);

	@Test
	public void messageSentOnAdd() {
		this.observer.increment(new Delta<Integer>("foo", 1));
		verify(this.channel).send(any(Message.class));
	}

	@Test
	public void messageSentOnSet() {
		this.observer.set(new Metric<Double>("foo", 1d));
		verify(this.channel).send(any(Message.class));
	}

}
