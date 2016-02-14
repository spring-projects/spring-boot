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

package org.springframework.boot.devtools.tunnel.server;

import java.io.IOException;
import java.nio.channels.ByteChannel;

/**
 * Manages the connection to the ultimate tunnel target server.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public interface TargetServerConnection {

	/**
	 * Open a connection to the target server with the specified timeout.
	 * @param timeout the read timeout
	 * @return a {@link ByteChannel} providing read/write access to the server
	 * @throws IOException in case of I/O errors
	 */
	ByteChannel open(int timeout) throws IOException;

}
