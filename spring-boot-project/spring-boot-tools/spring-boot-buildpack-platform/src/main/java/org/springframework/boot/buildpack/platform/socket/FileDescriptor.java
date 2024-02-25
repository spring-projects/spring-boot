/*
 * Copyright 2012-2021 the original author or authors.
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

import java.io.Closeable;
import java.io.IOException;
import java.util.function.IntConsumer;

/**
 * Provides access to the underlying file system representation of an open file.
 *
 * @author Phillip Webb
 * @see #acquire()
 */
class FileDescriptor {

	private final Handle openHandle;

	private final Handle closedHandler;

	private final IntConsumer closer;

	private Status status = Status.OPEN;

	private int referenceCount;

	/**
     * Constructs a new FileDescriptor with the specified handle and closer.
     * 
     * @param handle the handle associated with the file descriptor
     * @param closer the closer function to be called when the file descriptor is closed
     */
    FileDescriptor(int handle, IntConsumer closer) {
		this.openHandle = new Handle(handle);
		this.closedHandler = new Handle(-1);
		this.closer = closer;
	}

	/**
	 * Acquire an instance of the actual {@link Handle}. The caller must
	 * {@link Handle#close() close} the resulting handle when done.
	 * @return the handle
	 */
	synchronized Handle acquire() {
		this.referenceCount++;
		return (this.status != Status.OPEN) ? this.closedHandler : this.openHandle;
	}

	/**
     * Decreases the reference count of the FileDescriptor object and checks if it should be closed.
     * If the reference count reaches zero and the status is set to CLOSE_PENDING, the closer function is called
     * with the value of the open handle, and the status is set to CLOSED.
     */
    private synchronized void release() {
		this.referenceCount--;
		if (this.referenceCount == 0 && this.status == Status.CLOSE_PENDING) {
			this.closer.accept(this.openHandle.value);
			this.status = Status.CLOSED;
		}
	}

	/**
	 * Close the underlying file when all handles have been released.
	 */
	synchronized void close() {
		if (this.status == Status.OPEN) {
			if (this.referenceCount == 0) {
				this.closer.accept(this.openHandle.value);
				this.status = Status.CLOSED;
			}
			else {
				this.status = Status.CLOSE_PENDING;
			}
		}
	}

	/**
	 * The status of the file descriptor.
	 */
	private enum Status {

		OPEN, CLOSE_PENDING, CLOSED

	}

	/**
	 * Provides access to the actual file descriptor handle.
	 */
	final class Handle implements Closeable {

		private final int value;

		/**
         * Sets the value of the handle.
         * 
         * @param value the value to be set
         */
        private Handle(int value) {
			this.value = value;
		}

		/**
         * Checks if the handle is closed.
         * 
         * @return true if the handle is closed, false otherwise
         */
        boolean isClosed() {
			return this.value == -1;
		}

		/**
         * Returns the integer value of the Handle object.
         *
         * @return the integer value of the Handle object
         */
        int intValue() {
			return this.value;
		}

		/**
         * Closes the handle.
         * 
         * @throws IOException if an I/O error occurs while closing the handle
         */
        @Override
		public void close() throws IOException {
			if (!isClosed()) {
				release();
			}
		}

	}

}
