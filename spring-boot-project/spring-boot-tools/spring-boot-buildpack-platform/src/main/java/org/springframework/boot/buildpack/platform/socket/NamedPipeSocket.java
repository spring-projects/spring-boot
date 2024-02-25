/*
 * Copyright 2012-2024 the original author or authors.
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

	/**
     * Constructs a new NamedPipeSocket with the specified path.
     * 
     * @param path the path of the named pipe
     * @throws IOException if an I/O error occurs while opening the named pipe
     */
    NamedPipeSocket(String path) throws IOException {
		this.channel = open(path);
	}

	/**
     * Opens an asynchronous file byte channel for the specified path.
     * 
     * @param path the path of the file to open
     * @return the opened asynchronous file byte channel
     * @throws IOException if an I/O error occurs while opening the file
     */
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

	/**
     * Returns an input stream for reading from this NamedPipeSocket.
     *
     * @return the input stream for reading from this NamedPipeSocket.
     */
    @Override
	public InputStream getInputStream() {
		return Channels.newInputStream(this.channel);
	}

	/**
     * Returns an output stream for writing data to this NamedPipeSocket.
     *
     * @return the output stream for this NamedPipeSocket
     */
    @Override
	public OutputStream getOutputStream() {
		return Channels.newOutputStream(this.channel);
	}

	/**
     * Closes the NamedPipeSocket and releases any system resources associated with it.
     * If the NamedPipeSocket's channel is not null, it will be closed.
     *
     * @throws IOException if an I/O error occurs while closing the NamedPipeSocket or its channel
     */
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

		/**
         * Constructs a new AsynchronousFileByteChannel object with the specified AsynchronousFileChannel.
         *
         * @param fileChannel the AsynchronousFileChannel to be associated with the AsynchronousFileByteChannel
         */
        AsynchronousFileByteChannel(AsynchronousFileChannel fileChannel) {
			this.fileChannel = fileChannel;
		}

		/**
         * Reads a sequence of bytes from the file into the given buffer, and invokes the specified completion handler
         * when the operation completes.
         *
         * @param dst      The buffer into which bytes are to be transferred
         * @param attachment The object to attach to the I/O operation
         * @param handler The completion handler to invoke when the operation completes
         * @param <A> The type of the attachment
         */
        @Override
		public <A> void read(ByteBuffer dst, A attachment, CompletionHandler<Integer, ? super A> handler) {
			this.fileChannel.read(dst, 0, attachment, new CompletionHandler<>() {

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

		/**
         * Reads a sequence of bytes from this channel into the given buffer.
         * 
         * @param dst the buffer into which bytes are to be transferred
         * @return a Future representing the result of the asynchronous operation
         * @throws IllegalArgumentException if the buffer is null
         * @throws ReadPendingException if a read operation is already in progress on this channel
         * @throws NonReadableChannelException if this channel was not opened for reading
         * @throws ClosedChannelException if this channel is closed
         * @throws IOException if an I/O error occurs
         */
        @Override
		public Future<Integer> read(ByteBuffer dst) {
			CompletableFutureHandler future = new CompletableFutureHandler();
			this.fileChannel.read(dst, 0, null, future);
			return future;
		}

		/**
         * Writes a sequence of bytes to this channel from the given buffer, starting at the current position.
         * The operation is asynchronous and will be completed by invoking the given completion handler.
         *
         * @param src The buffer from which bytes are to be transferred
         * @param attachment The object to attach to the I/O operation; can be null
         * @param handler The completion handler to invoke when the operation is completed
         * @param <A> The type of the attachment
         *
         * @throws IllegalArgumentException If the buffer is null
         * @throws NonWritableChannelException If this channel was not opened for writing
         * @throws AsynchronousCloseException If another thread closes this channel while the write operation is in progress
         * @throws ClosedChannelException If this channel is closed
         * @throws IOException If some other I/O error occurs
         */
        @Override
		public <A> void write(ByteBuffer src, A attachment, CompletionHandler<Integer, ? super A> handler) {
			this.fileChannel.write(src, 0, attachment, handler);
		}

		/**
         * Writes a sequence of bytes to the file channel from the given buffer.
         * 
         * @param src the buffer containing the bytes to be written
         * @return a Future representing the result of the write operation
         * @throws IOException if an I/O error occurs
         */
        @Override
		public Future<Integer> write(ByteBuffer src) {
			return this.fileChannel.write(src, 0);
		}

		/**
         * Closes the AsynchronousFileByteChannel and releases any system resources associated with it.
         * 
         * @throws IOException if an I/O error occurs while closing the channel
         */
        @Override
		public void close() throws IOException {
			this.fileChannel.close();
		}

		/**
         * Returns a boolean value indicating whether the file channel associated with this asynchronous file byte channel is open.
         *
         * @return {@code true} if the file channel is open, {@code false} otherwise.
         */
        @Override
		public boolean isOpen() {
			return this.fileChannel.isOpen();
		}

		/**
         * CompletableFutureHandler class.
         */
        private static final class CompletableFutureHandler extends CompletableFuture<Integer>
				implements CompletionHandler<Integer, Object> {

			/**
             * This method is called when the asynchronous read operation is completed.
             * It takes in the number of bytes read and the attachment object as parameters.
             * If the number of bytes read is greater than 0, it calls the complete method with the number of bytes read.
             * Otherwise, it calls the complete method with -1.
             * 
             * @param read The number of bytes read in the asynchronous read operation.
             * @param attachment The attachment object associated with the asynchronous read operation.
             */
            @Override
			public void completed(Integer read, Object attachment) {
				complete((read > 0) ? read : -1);
			}

			/**
             * This method is called when the asynchronous operation fails.
             * 
             * @param exc        The exception that caused the failure.
             * @param attachment The object attached to the asynchronous operation.
             */
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
	private static final class SleepAwaiter implements Consumer<String> {

		/**
         * Pauses the execution for a specified interval of time.
         * 
         * @param path the path to be accepted
         */
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
	private static final class WindowsAwaiter implements Consumer<String> {

		/**
         * Waits for a named pipe to become available.
         * 
         * @param path the path of the named pipe to wait for
         */
        @Override
		public void accept(String path) {
			Kernel32.INSTANCE.WaitNamedPipe(path, WAIT_INTERVAL);
		}

	}

}
