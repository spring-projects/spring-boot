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

package org.springframework.boot.devtools.tunnel.payload;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Utility class that forwards {@link HttpTunnelPayload} instances to a destination
 * channel, respecting sequence order.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class HttpTunnelPayloadForwarder {

	private static final int MAXIMUM_QUEUE_SIZE = 100;

	private final Map<Long, HttpTunnelPayload> queue = new HashMap<Long, HttpTunnelPayload>();

	private final Object monitor = new Object();

	private final WritableByteChannel targetChannel;

	private long lastRequestSeq = 0;

	/**
	 * Create a new {@link HttpTunnelPayloadForwarder} instance.
	 * @param targetChannel the target channel
	 */
	public HttpTunnelPayloadForwarder(WritableByteChannel targetChannel) {
		Assert.notNull(targetChannel, "TargetChannel must not be null");
		this.targetChannel = targetChannel;
	}

	public void forward(HttpTunnelPayload payload) throws IOException {
		synchronized (this.monitor) {
			long seq = payload.getSequence();
			if (this.lastRequestSeq != seq - 1) {
				Assert.state(this.queue.size() < MAXIMUM_QUEUE_SIZE,
						"Too many messages queued");
				this.queue.put(seq, payload);
				return;
			}
			payload.logOutgoing();
			payload.writeTo(this.targetChannel);
			this.lastRequestSeq = seq;
			HttpTunnelPayload queuedItem = this.queue.get(seq + 1);
			if (queuedItem != null) {
				forward(queuedItem);
			}
		}
	}

}
