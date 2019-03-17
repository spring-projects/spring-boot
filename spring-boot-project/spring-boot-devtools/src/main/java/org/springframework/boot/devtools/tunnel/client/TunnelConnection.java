/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.devtools.tunnel.client;

import java.io.Closeable;
import java.nio.channels.WritableByteChannel;

/**
 * Interface used to manage socket tunnel connections.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
@FunctionalInterface
public interface TunnelConnection {

	/**
	 * Open the tunnel connection.
	 * @param incomingChannel a {@link WritableByteChannel} that should be used to write
	 * any incoming data received from the remote server
	 * @param closeable a closeable to call when the channel is closed
	 * @return a {@link WritableByteChannel} that should be used to send any outgoing data
	 * destined for the remote server
	 * @throws Exception in case of errors
	 */
	WritableByteChannel open(WritableByteChannel incomingChannel, Closeable closeable)
			throws Exception;

}
