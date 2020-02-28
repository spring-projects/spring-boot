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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Structure;

import org.springframework.util.Assert;

/**
 * {@link DomainSocket} implementation for Linux based platforms.
 *
 * @author Phillip Webb
 */
class LinuxDomainSocket extends DomainSocket {

	static {
		Native.register(Platform.C_LIBRARY_NAME);
	}

	LinuxDomainSocket(String path) throws IOException {
		super(path);
	}

	private static final int MAX_PATH_LENGTH = 108;

	@Override
	protected void connect(String path, int handle) {
		SockaddrUn address = new SockaddrUn(AF_LOCAL, path.getBytes(StandardCharsets.UTF_8));
		connect(handle, address, address.size());
	}

	private native int connect(int fd, SockaddrUn address, int addressLen) throws LastErrorException;

	/**
	 * Native {@code sockaddr_un} structure as defined in {@code sys/un.h}.
	 */
	public static class SockaddrUn extends Structure implements Structure.ByReference {

		public short sunFamily;

		public byte[] sunPath = new byte[MAX_PATH_LENGTH];

		private SockaddrUn(byte sunFamily, byte[] path) {
			Assert.isTrue(path.length < MAX_PATH_LENGTH, () -> "Path cannot exceed " + MAX_PATH_LENGTH + " bytes");
			System.arraycopy(path, 0, this.sunPath, 0, path.length);
			this.sunPath[path.length] = 0;
			this.sunFamily = sunFamily;
			allocateMemory();
		}

		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList(new String[] { "sunFamily", "sunPath" });
		}

	}

}
