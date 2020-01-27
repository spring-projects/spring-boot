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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;

/**
 * A {@link Socket} implementation for named pipes.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public class NamedPipeSocket extends Socket {

	private static final int WAIT_INTERVAL = 100;

	private static final long TIMEOUT = TimeUnit.MILLISECONDS.toNanos(1000);

	private final RandomAccessFile file;

	private final InputStream inputStream;

	private final OutputStream outputStream;

	NamedPipeSocket(String path) throws IOException {
		this.file = open(path);
		this.inputStream = new NamedPipeInputStream();
		this.outputStream = new NamedPipeOutputStream();
	}

	private static RandomAccessFile open(String path) throws IOException {
		Consumer<String> awaiter = Platform.isWindows() ? new WindowsAwaiter() : new SleepAwaiter();
		long startTime = System.nanoTime();
		while (true) {
			try {
				return new RandomAccessFile(path, "rw");
			}
			catch (FileNotFoundException ex) {
				if (System.nanoTime() - startTime > TIMEOUT) {
					throw ex;
				}
				awaiter.accept(path);
			}
		}
	}

	@Override
	public InputStream getInputStream() {
		return this.inputStream;
	}

	@Override
	public OutputStream getOutputStream() {
		return this.outputStream;
	}

	@Override
	public void close() throws IOException {
		this.file.close();
	}

	protected final RandomAccessFile getFile() {
		return this.file;
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
	 * {@link InputStream} returned from the {@link NamedPipeSocket}.
	 */
	private class NamedPipeInputStream extends InputStream {

		@Override
		public int read() throws IOException {
			return getFile().read();
		}

		@Override
		public int read(byte[] bytes, int off, int len) throws IOException {
			return getFile().read(bytes, off, len);
		}

	}

	/**
	 * {@link InputStream} returned from the {@link NamedPipeSocket}.
	 */
	private class NamedPipeOutputStream extends OutputStream {

		@Override
		public void write(int value) throws IOException {
			NamedPipeSocket.this.file.write(value);
		}

		@Override
		public void write(byte[] bytes, int off, int len) throws IOException {
			NamedPipeSocket.this.file.write(bytes, off, len);
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
