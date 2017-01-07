/*
 * Copyright 2012-2016 the original author or authors.
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

package samples.websocket.jetty.client;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.websocket.Extension;

import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.common.LogicalConnection;
import org.eclipse.jetty.websocket.common.WebSocketSession;
import org.eclipse.jetty.websocket.common.events.EventDriver;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.eclipse.jetty.websocket.jsr356.JsrSession;
import org.eclipse.jetty.websocket.jsr356.JsrSessionFactory;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * Jetty {@link ClientContainer} to work around
 * https://github.com/eclipse/jetty.project/issues/1202.
 *
 * @author Phillip Webb
 */
public class FixedClientContainer extends ClientContainer {

	public FixedClientContainer() {
		super();
		WebSocketClient client = getClient();
		ReflectionTestUtils.setField(client, "sessionFactory",
				new FixedJsrSessionFactory(this));
	}

	private static class FixedJsrSessionFactory extends JsrSessionFactory {

		private final ClientContainer container;

		public FixedJsrSessionFactory(ClientContainer container) {
			super(container);
			this.container = container;
		}

		@Override
		public WebSocketSession createSession(URI requestURI, EventDriver websocket,
				LogicalConnection connection) {
			return new FixedJsrSession(this.container, connection.getId(), requestURI,
					websocket, connection);
		}

	}

	private static class FixedJsrSession extends JsrSession {

		public FixedJsrSession(ClientContainer container, String id, URI requestURI,
				EventDriver websocket, LogicalConnection connection) {
			super(container, id, requestURI, websocket, connection);
		}

		@Override
		public List<Extension> getNegotiatedExtensions() {
			try {
				return super.getNegotiatedExtensions();
			}
			catch (NullPointerException ex) {
				return Collections.emptyList();
			}
		}

	}

}
