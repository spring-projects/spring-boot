/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.springframework.util.Assert;

/**
 * An {@link InetAddressFilter} that matches internal (private) addresses.
 * <p>
 * Internal addresses include loopback addresses (127.0.0.0/8 for IPv4, ::1 for IPv6),
 * private IPv4 address ranges (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16), and IPv6
 * Unique Local Addresses (fc00::/7).
 *
 * @author Gábor Vaspöri
 * @author Kian Jamali
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Phillip Webb
 */
final class InternalInetAddressFilter implements InetAddressFilter {

	private static final byte[] NAT64_PREFIX = { (byte) 0x00, (byte) 0x64, (byte) 0xff, (byte) 0x9b };

	static final InternalInetAddressFilter instance = new InternalInetAddressFilter();

	private InternalInetAddressFilter() {
	}

	@Override
	public boolean matches(InetAddress address) {
		Assert.notNull(address, "'address' must not be null");
		return isLocal(address) || isSiteLocalIpv6Address(address.getAddress());
	}

	/**
	 * Check for Unique Local IPv6 Addresses. We cannot rely on
	 * {@code Inet6Address.isSiteLocalAddress()} because the JVM implementation dictates
	 * that {@code fec0::/10} is the only site-local IPv6 address space, based on the
	 * outdated RFC 2373. That RFC was deprecated by the IETF in 2004 in favor of
	 * {@code fc00::/7} (RFC 4193). To keep our private network checking accurate to
	 * modern subnets, we maintain manual parsing.
	 * @param address the address to check
	 * @return if the addess is site local
	 */
	private boolean isSiteLocalIpv6Address(byte[] address) {
		return (address.length == 16)
				&& (address[0] == (byte) 0xfc || address[0] == (byte) 0xfd || isNat64Local(address));
	}

	private boolean isNat64Local(byte[] address) {
		if (!Arrays.equals(address, 0, NAT64_PREFIX.length, NAT64_PREFIX, 0, NAT64_PREFIX.length)) {
			return false;
		}
		try { // IPv4/IPv6 translation, 64:ff9b
			return isLocal(InetAddress.getByAddress(Arrays.copyOfRange(address, 12, 16)));
		}
		catch (UnknownHostException ex) {
			return false; // Should not happen for 4-byte array
		}
	}

	private boolean isLocal(InetAddress address) {
		return address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress();
	}

}
