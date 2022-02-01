/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.devtools.tunnel.client;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.devtools.test.MockClientHttpRequestFactory;
import org.springframework.boot.devtools.tunnel.client.HttpTunnelConnection.TunnelChannel;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link HttpTunnelConnection}.
 *
 * @author Phillip Webb
 * @author Rob Winch
 * @author Andy Wilkinson
 */
@ExtendWith({ OutputCaptureExtension.class, MockitoExtension.class })
class HttpTunnelConnectionTests {

	private String url;

	private ByteArrayOutputStream incomingData;

	private WritableByteChannel incomingChannel;

	@Mock
	private Closeable closeable;

	private MockClientHttpRequestFactory requestFactory = new MockClientHttpRequestFactory();

	@BeforeEach
	void setup() {
		this.url = "http://localhost:12345";
		this.incomingData = new ByteArrayOutputStream();
		this.incomingChannel = Channels.newChannel(this.incomingData);
	}

	@Test
	void urlMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new HttpTunnelConnection(null, this.requestFactory))
				.withMessageContaining("URL must not be empty");
	}

	@Test
	void urlMustNotBeEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> new HttpTunnelConnection("", this.requestFactory))
				.withMessageContaining("URL must not be empty");
	}

	@Test
	void urlMustNotBeMalformed() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new HttpTunnelConnection("htttttp:///ttest", this.requestFactory))
				.withMessageContaining("Malformed URL 'htttttp:///ttest'");
	}

	@Test
	void requestFactoryMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new HttpTunnelConnection(this.url, null))
				.withMessageContaining("RequestFactory must not be null");
	}

	@Test
	void closeTunnelChangesIsOpen() throws Exception {
		this.requestFactory.willRespondAfterDelay(1000, HttpStatus.GONE);
		WritableByteChannel channel = openTunnel(false);
		assertThat(channel.isOpen()).isTrue();
		channel.close();
		assertThat(channel.isOpen()).isFalse();
	}

	@Test
	void closeTunnelCallsCloseableOnce() throws Exception {
		this.requestFactory.willRespondAfterDelay(1000, HttpStatus.GONE);
		WritableByteChannel channel = openTunnel(false);
		then(this.closeable).should(never()).close();
		channel.close();
		channel.close();
		then(this.closeable).should().close();
	}

	@Test
	void typicalTraffic() throws Exception {
		this.requestFactory.willRespond("hi", "=2", "=3");
		TunnelChannel channel = openTunnel(true);
		write(channel, "hello");
		write(channel, "1+1");
		write(channel, "1+2");
		assertThat(this.incomingData.toString()).isEqualTo("hi=2=3");
	}

	@Test
	void trafficWithLongPollTimeouts() throws Exception {
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
	void connectFailureLogsWarning(CapturedOutput output) throws Exception {
		this.requestFactory.willRespond(new ConnectException());
		TunnelChannel tunnel = openTunnel(true);
		assertThat(tunnel.isOpen()).isFalse();
		assertThat(output).contains("Failed to connect to remote application at http://localhost:12345");
	}

	private void write(TunnelChannel channel, String string) throws IOException {
		channel.write(ByteBuffer.wrap(string.getBytes()));
	}

	private TunnelChannel openTunnel(boolean singleThreaded) throws Exception {
		HttpTunnelConnection connection = new HttpTunnelConnection(this.url, this.requestFactory,
				singleThreaded ? new CurrentThreadExecutor() : null);
		return connection.open(this.incomingChannel, this.closeable);
	}

	static class CurrentThreadExecutor implements Executor {

		@Override
		public void execute(Runnable command) {
			command.run();
		}

	}

}
