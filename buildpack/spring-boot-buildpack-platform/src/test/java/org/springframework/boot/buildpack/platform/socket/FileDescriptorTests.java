/*
 * Copyright 2012-present the original author or authors.
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

	private final int sourceHandle = 123;

	private int closedHandle;

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
			assertThat(this.closedHandle).isZero();
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
