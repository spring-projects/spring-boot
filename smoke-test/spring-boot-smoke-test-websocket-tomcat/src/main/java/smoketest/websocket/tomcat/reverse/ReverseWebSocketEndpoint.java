/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.websocket.tomcat.reverse;

import java.io.IOException;

import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/reverse")
public class ReverseWebSocketEndpoint {

	@OnMessage
	public void handleMessage(Session session, String message) throws IOException {
		session.getBasicRemote().sendText("Reversed: " + new StringBuilder(message).reverse());
	}

}
