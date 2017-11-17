/*
 * Copyright 2012-2017 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.devtools.test.MockClientHttpRequestFactory;
import org.springframework.boot.devtools.tunnel.client.HttpTunnelConnection.TunnelChannel;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link HttpTunnelConnection}.
 *
 * @author Phillip Webb
 * @author Rob Winch
 * @author Andy Wilkinson
 */
public class HttpTunnelConnectionTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	private String url;

	private ByteArrayOutputStream incomingData;

	private WritableByteChannel incomingChannel;

	@Mock
	private Closeable closeable;

	private MockClientHttpRequestFactory requestFactory = new MockClientHttpRequestFactory();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.url = "http://localhost:12345";
		this.incomingData = new ByteArrayOutputStream();
		this.incomingChannel = Channels.newChannel(this.incomingData);
	}

	@Test
	public void urlMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("URL must not be empty");
		new HttpTunnelConnection(null, this.requestFactory);
	}

	@Test
	public void urlMustNotBeEmpty() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("URL must not be empty");
		new HttpTunnelConnection("", this.requestFactory);
	}

	@Test
	public void urlMustNotBeMalformed() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Malformed URL 'htttttp:///ttest'");
		new HttpTunnelConnection("htttttp:///ttest", this.requestFactory);
	}

	@Test
	public void requestFactoryMustNotBeNull() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("RequestFactory must not be null");
		new HttpTunnelConnection(this.url, null);
	}

	@Test
	public void closeTunnelChangesIsOpen() throws Exception {
		this.requestFactory.willRespondAfterDelay(1000, HttpStatus.GONE);
		WritableByteChannel channel = openTunnel(false);
		assertThat(channel.isOpen()).isTrue();
		channel.close();
		assertThat(channel.isOpen()).isFalse();
	}

	@Test
	public void closeTunnelCallsCloseableOnce() throws Exception {
		this.requestFactory.willRespondAfterDelay(1000, HttpStatus.GONE);
		WritableByteChannel channel = openTunnel(false);
		verify(this.closeable, never()).close();
		channel.close();
		channel.close();
		verify(this.closeable, times(1)).close();
	}

	@Test
	public void typicalTraffic() throws Exception {
		this.requestFactory.willRespond("hi", "=2", "=3");
		TunnelChannel channel = openTunnel(true);
		write(channel, "hello");
		write(channel, "1+1");
		write(channel, "1+2");
		assertThat(this.incomingData.toString()).isEqualTo("hi=2=3");
	}

	@Test
	public void trafficWithLongPollTimeouts() throws Exception {
		for (int i = 0; i < 10; i++) {
			this.requestFactory.willRespond(HttpStatus.NO_CONTENT);
		}
		this.requestFactory.willRespond("hi");
		TunnelChannel channel = openTunnel(true);
		write(channel, "hello");
		assertThat(this.incomingData.toString()).isEqualTo("hi");
		assertThat(this.requestFactory.getExecutedRequests().size()).isGreaterThan(10);
	}

	@Test
	public void connectFailureLogsWarning() throws Exception {
		this.requestFactory.willRespond(new ConnectException());
		TunnelChannel tunnel = openTunnel(true);
		assertThat(tunnel.isOpen()).isFalse();
		this.outputCapture.expect(containsString(
				"Failed to connect to remote application at http://localhost:12345"));
	}

	private void write(TunnelChannel channel, String string) throws IOException {
		channel.write(ByteBuffer.wrap(string.getBytes()));
	}

	private TunnelChannel openTunnel(boolean singleThreaded) throws Exception {
		HttpTunnelConnection connection = new HttpTunnelConnection(this.url,
				this.requestFactory,
				(singleThreaded ? new CurrentThreadExecutor() : null));
		return connection.open(this.incomingChannel, this.closeable);
	}

	private static class CurrentThreadExecutor implements Executor {

		@Override
		public void execute(Runnable command) {
			command.run();
		}

	}

}
