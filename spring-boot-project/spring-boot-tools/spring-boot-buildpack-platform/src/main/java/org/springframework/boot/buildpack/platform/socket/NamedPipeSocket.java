/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.buildpack.platform.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.Channels;
import java.nio.channels.CompletionHandler;
import java.nio.file.FileSystemException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;

/**
 * A {@link Socket} implementation for named pipes.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 */
public class NamedPipeSocket extends Socket {

	private static final int WAIT_INTERVAL = 100;

	private static final long TIMEOUT = TimeUnit.MILLISECONDS.toNanos(1000);

	private final AsynchronousFileByteChannel channel;

	NamedPipeSocket(String path) throws IOException {
		this.channel = open(path);
	}

	private AsynchronousFileByteChannel open(String path) throws IOException {
		Consumer<String> awaiter = Platform.isWindows() ? new WindowsAwaiter() : new SleepAwaiter();
		long startTime = System.nanoTime();
		while (true) {
			try {
				return new AsynchronousFileByteChannel(AsynchronousFileChannel.open(Paths.get(path),
						StandardOpenOption.READ, StandardOpenOption.WRITE));
			}
			catch (FileSystemException ex) {
				if (System.nanoTime() - startTime >= TIMEOUT) {
					throw ex;
				}
				awaiter.accept(path);
			}
		}
	}

	@Override
	public InputStream getInputStream() {
		return Channels.newInputStream(this.channel);
	}

	@Override
	public OutputStream getOutputStream() {
		return Channels.newOutputStream(this.channel);
	}

	@Override
	public void close() throws IOException {
		if (this.channel != null) {
			this.channel.close();
		}
	}

	/**
	 * Return a new {@link NamedPipeSocket} for the given path.
	 * @param path the path to the domain socket
	 * @return a {@link NamedPipeSocket} instance
	 * @throws IOException if the socket cannot be opened
	 */
	public static NamedPipeSocket get(String path) throws IOException {
		return new NamedPipeSocket(path);
	}

	/**
	 * Adapt an {@code AsynchronousByteChannel} to an {@code AsynchronousFileChannel}.
	 */
	private static class AsynchronousFileByteChannel implements AsynchronousByteChannel {

		private final AsynchronousFileChannel fileChannel;

		AsynchronousFileByteChannel(AsynchronousFileChannel fileChannel) {
			this.fileChannel = fileChannel;
		}

		@Override
		public <A> void read(ByteBuffer dst, A attachment, CompletionHandler<Integer, ? super A> handler) {
			this.fileChannel.read(dst, 0, attachment, new CompletionHandler<Integer, A>() {

				@Override
				public void completed(Integer read, A attachment) {
					handler.completed((read > 0) ? read : -1, attachment);
				}

				@Override
				public void failed(Throwable exc, A attachment) {
					if (exc instanceof AsynchronousCloseException) {
						handler.completed(-1, attachment);
						return;
					}
					handler.failed(exc, attachment);
				}
			});

		}

		@Override
		public Future<Integer> read(ByteBuffer dst) {
			CompletableFutureHandler future = new CompletableFutureHandler();
			this.fileChannel.read(dst, 0, null, future);
			return future;
		}

		@Override
		public <A> void write(ByteBuffer src, A attachment, CompletionHandler<Integer, ? super A> handler) {
			this.fileChannel.write(src, 0, attachment, handler);
		}

		@Override
		public Future<Integer> write(ByteBuffer src) {
			return this.fileChannel.write(src, 0);
		}

		@Override
		public void close() throws IOException {
			this.fileChannel.close();
		}

		@Override
		public boolean isOpen() {
			return this.fileChannel.isOpen();
		}

		private static class CompletableFutureHandler extends CompletableFuture<Integer>
				implements CompletionHandler<Integer, Object> {

			@Override
			public void completed(Integer read, Object attachment) {
				complete((read > 0) ? read : -1);
			}

			@Override
			public void failed(Throwable exc, Object attachment) {
				if (exc instanceof AsynchronousCloseException) {
					complete(-1);
					return;
				}
				completeExceptionally(exc);
			}

		}

	}

	/**
	 * Waits for the name pipe file using a simple sleep.
	 */
	private static class SleepAwaiter implements Consumer<String> {

		@Override
		public void accept(String path) {
			try {
				Thread.sleep(WAIT_INTERVAL);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

	}

	/**
	 * Waits for the name pipe file using Windows specific logic.
	 */
	private static class WindowsAwaiter implements Consumer<String> {

		@Override
		public void accept(String path) {
			Kernel32.INSTANCE.WaitNamedPipe(path, WAIT_INTERVAL);
		}

	}

}
