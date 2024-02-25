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

	/**
	 * Constructs a new LinuxDomainSocket with the specified path.
	 * @param path the path of the domain socket
	 * @throws IOException if an I/O error occurs while creating the domain socket
	 */
	LinuxDomainSocket(String path) throws IOException {
		super(path);
	}

	private static final int MAX_PATH_LENGTH = 108;

	/**
	 * Connects to a Unix domain socket using the given path and handle.
	 * @param path the path of the Unix domain socket
	 * @param handle the handle of the Unix domain socket
	 */
	@Override
	protected void connect(String path, int handle) {
		SockaddrUn address = new SockaddrUn(AF_LOCAL, path.getBytes(StandardCharsets.UTF_8));
		connect(handle, address, address.size());
	}

	/**
	 * Connects a Linux domain socket to the specified address.
	 * @param fd the file descriptor of the socket to connect
	 * @param address the Unix domain socket address to connect to
	 * @param addressLen the length of the address structure
	 * @return the result of the connection attempt
	 * @throws LastErrorException if an error occurs during the connection attempt
	 */
	private native int connect(int fd, SockaddrUn address, int addressLen) throws LastErrorException;

	/**
	 * Native {@code sockaddr_un} structure as defined in {@code sys/un.h}.
	 */
	public static class SockaddrUn extends Structure implements Structure.ByReference {

		public short sunFamily;

		public byte[] sunPath = new byte[MAX_PATH_LENGTH];

		/**
		 * Constructs a new SockaddrUn object with the specified sunFamily and path.
		 * @param sunFamily the sunFamily value to set
		 * @param path the path value to set
		 * @throws IllegalArgumentException if the length of the path exceeds
		 * MAX_PATH_LENGTH
		 */
		private SockaddrUn(byte sunFamily, byte[] path) {
			Assert.isTrue(path.length < MAX_PATH_LENGTH, () -> "Path cannot exceed " + MAX_PATH_LENGTH + " bytes");
			System.arraycopy(path, 0, this.sunPath, 0, path.length);
			this.sunPath[path.length] = 0;
			this.sunFamily = sunFamily;
			allocateMemory();
		}

		/**
		 * Returns the field order of the SockaddrUn class.
		 * @return a list of field names in the order they appear in the class
		 */
		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("sunFamily", "sunPath");
		}

	}

}
