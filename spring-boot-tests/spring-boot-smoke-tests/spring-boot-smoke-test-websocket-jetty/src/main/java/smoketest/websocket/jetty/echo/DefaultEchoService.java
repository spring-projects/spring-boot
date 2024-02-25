/*
 * Copyright 2012-2019 the original author or authors.
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

package smoketest.websocket.jetty.echo;

/**
 * DefaultEchoService class.
 */
public class DefaultEchoService implements EchoService {

	private final String echoFormat;

	/**
	 * Constructs a new DefaultEchoService with the specified echo format.
	 * @param echoFormat the format string used for echoing messages
	 */
	public DefaultEchoService(String echoFormat) {
		this.echoFormat = (echoFormat != null) ? echoFormat : "%s";
	}

	/**
	 * Returns the formatted message using the provided message and echo format.
	 * @param message the message to be formatted
	 * @return the formatted message
	 */
	@Override
	public String getMessage(String message) {
		return String.format(this.echoFormat, message);
	}

}
