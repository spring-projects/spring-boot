/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.devtools.livereload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;

/**
 * A {@link LiveReloadServer} connection.
 *
 * @author Phillip Webb
 * @author Francis Lavoie
 */
class Connection {

	private static final Log logger = LogFactory.getLog(Connection.class);

	private static final Pattern WEBSOCKET_KEY_PATTERN = Pattern.compile("^sec-websocket-key:(.*)$",
			Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

	public static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

	private final Socket socket;

	private final ConnectionInputStream inputStream;

	private final ConnectionOutputStream outputStream;

	private final String header;

	private volatile boolean webSocket;

	private volatile boolean running = true;

	/**
	 * Create a new {@link Connection} instance.
	 * @param socket the source socket
	 * @param inputStream the socket input stream
	 * @param outputStream the socket output stream
	 * @throws IOException in case of I/O errors
	 */
	Connection(Socket socket, InputStream inputStream, OutputStream outputStream) throws IOException {
		this.socket = socket;
		this.inputStream = new ConnectionInputStream(inputStream);
		this.outputStream = new ConnectionOutputStream(outputStream);
		String header = this.inputStream.readHeader();
		logger.debug(LogMessage.format("Established livereload connection [%s]", header));
		this.header = header;
	}

	/**
	 * Run the connection.
	 * @throws Exception in case of errors
	 */
	void run() throws Exception {
		String lowerCaseHeader = this.header.toLowerCase();
		if (lowerCaseHeader.contains("upgrade: websocket") && lowerCaseHeader.contains("sec-websocket-version: 13")) {
			runWebSocket();
		}
		if (lowerCaseHeader.contains("get /livereload.js")) {
			this.outputStream.writeHttp(getClass().getResourceAsStream("livereload.js"), "text/javascript");
		}
	}

	/**
	 * Runs the WebSocket connection.
	 * @throws Exception if an error occurs during the WebSocket connection.
	 */
	private void runWebSocket() throws Exception {
		this.webSocket = true;
		String accept = getWebsocketAcceptResponse();
		this.outputStream.writeHeaders("HTTP/1.1 101 Switching Protocols", "Upgrade: websocket", "Connection: Upgrade",
				"Sec-WebSocket-Accept: " + accept);
		new Frame("{\"command\":\"hello\",\"protocols\":[\"http://livereload.com/protocols/official-7\"],"
				+ "\"serverName\":\"spring-boot\"}")
			.write(this.outputStream);
		while (this.running) {
			readWebSocketFrame();
		}
	}

	/**
	 * Reads a WebSocket frame from the input stream and handles it accordingly. If the
	 * frame type is PING, a PONG frame is written back to the output stream. If the frame
	 * type is CLOSE, a ConnectionClosedException is thrown. If the frame type is TEXT, a
	 * debug log message is generated. If the frame type is unexpected, an IOException is
	 * thrown. If a SocketTimeoutException occurs, a PING frame is written back to the
	 * output stream, followed by reading a PONG frame from the input stream. If no PONG
	 * frame is received, an IllegalStateException is thrown.
	 * @throws IOException if an I/O error occurs while reading or writing the WebSocket
	 * frame
	 * @throws ConnectionClosedException if the WebSocket connection is closed
	 * @throws IllegalStateException if no PONG frame is received after sending a PING
	 * frame
	 */
	private void readWebSocketFrame() throws IOException {
		try {
			Frame frame = Frame.read(this.inputStream);
			if (frame.getType() == Frame.Type.PING) {
				writeWebSocketFrame(new Frame(Frame.Type.PONG));
			}
			else if (frame.getType() == Frame.Type.CLOSE) {
				throw new ConnectionClosedException();
			}
			else if (frame.getType() == Frame.Type.TEXT) {
				logger.debug(LogMessage.format("Received LiveReload text frame %s", frame));
			}
			else {
				throw new IOException("Unexpected Frame Type " + frame.getType());
			}
		}
		catch (SocketTimeoutException ex) {
			writeWebSocketFrame(new Frame(Frame.Type.PING));
			Frame frame = Frame.read(this.inputStream);
			if (frame.getType() != Frame.Type.PONG) {
				throw new IllegalStateException("No Pong");
			}
		}
	}

	/**
	 * Trigger livereload for the client using this connection.
	 * @throws IOException in case of I/O errors
	 */
	void triggerReload() throws IOException {
		if (this.webSocket) {
			logger.debug("Triggering LiveReload");
			writeWebSocketFrame(new Frame("{\"command\":\"reload\",\"path\":\"/\"}"));
		}
	}

	/**
	 * Writes a WebSocket frame to the output stream.
	 * @param frame the WebSocket frame to be written
	 * @throws IOException if an I/O error occurs while writing the frame
	 */
	private void writeWebSocketFrame(Frame frame) throws IOException {
		frame.write(this.outputStream);
	}

	/**
	 * Generates the WebSocket accept response for the connection.
	 * @return the WebSocket accept response as a string
	 * @throws NoSuchAlgorithmException if the SHA-1 algorithm is not available
	 */
	private String getWebsocketAcceptResponse() throws NoSuchAlgorithmException {
		Matcher matcher = WEBSOCKET_KEY_PATTERN.matcher(this.header);
		Assert.state(matcher.find(), "No Sec-WebSocket-Key");
		String response = matcher.group(1).trim() + WEBSOCKET_GUID;
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
		messageDigest.update(response.getBytes(), 0, response.length());
		return Base64.getEncoder().encodeToString(messageDigest.digest());
	}

	/**
	 * Close the connection.
	 * @throws IOException in case of I/O errors
	 */
	void close() throws IOException {
		this.running = false;
		this.socket.close();
	}

}
