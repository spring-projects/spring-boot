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

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.socket.FileDescriptor.Handle;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link FileDescriptor}.
 *
 * @author Phillip Webb
 */
class FileDescriptorTests {

	private int sourceHandle = 123;

	private int closedHandle = 0;

	@Test
	void acquireReturnsHandle() throws Exception {
		FileDescriptor descriptor = new FileDescriptor(this.sourceHandle, this::close);
		try (Handle handle = descriptor.acquire()) {
			assertThat(handle.intValue()).isEqualTo(this.sourceHandle);
			assertThat(handle.isClosed()).isFalse();
		}
	}

	@Test
	void acquireWhenClosedReturnsClosedHandle() throws Exception {
		FileDescriptor descriptor = new FileDescriptor(this.sourceHandle, this::close);
		descriptor.close();
		try (Handle handle = descriptor.acquire()) {
			assertThat(handle.intValue()).isEqualTo(-1);
			assertThat(handle.isClosed()).isTrue();
		}
	}

	@Test
	void acquireWhenPendingCloseReturnsClosedHandle() throws Exception {
		FileDescriptor descriptor = new FileDescriptor(this.sourceHandle, this::close);
		try (Handle handle1 = descriptor.acquire()) {
			descriptor.close();
			try (Handle handle2 = descriptor.acquire()) {
				assertThat(handle2.intValue()).isEqualTo(-1);
				assertThat(handle2.isClosed()).isTrue();
			}
		}
	}

	@Test
	void finalizeTriggersClose() {
		FileDescriptor descriptor = new FileDescriptor(this.sourceHandle, this::close);
		descriptor.close();
		assertThat(this.closedHandle).isEqualTo(this.sourceHandle);
	}

	@Test
	void closeWhenHandleAcquiredClosesOnRelease() throws Exception {
		FileDescriptor descriptor = new FileDescriptor(this.sourceHandle, this::close);
		try (Handle handle = descriptor.acquire()) {
			descriptor.close();
			assertThat(this.closedHandle).isEqualTo(0);
		}
		assertThat(this.closedHandle).isEqualTo(this.sourceHandle);
	}

	@Test
	void closeWhenHandleNotAcquiredClosesImmediately() {
		FileDescriptor descriptor = new FileDescriptor(this.sourceHandle, this::close);
		descriptor.close();
		assertThat(this.closedHandle).isEqualTo(this.sourceHandle);
	}

	private void close(int handle) {
		this.closedHandle = handle;
	}

}
