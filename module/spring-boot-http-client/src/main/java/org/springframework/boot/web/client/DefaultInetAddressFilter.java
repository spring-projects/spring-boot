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

package org.springframework.boot.web.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

public final class DefaultInetAddressFilter implements InetAddressFilter {

	private final List<IpAddressMatcher> allowList;

	private final List<IpAddressMatcher> denyList;

	private final @Nullable BlockMode blockMode;

	private final List<InetAddressFilter> customFilters;

	public DefaultInetAddressFilter(List<String> allowList, List<String> denyList, boolean blockExternal,
			boolean blockInternal, List<InetAddressFilter> customFilters) {

		if (!allowList.isEmpty() && !denyList.isEmpty()) {
			throw new IllegalArgumentException(
					"Logic inconsistency: allowList and denyList cannot be used at the same time");
		}

		this.allowList = initIpList(allowList);
		this.denyList = initIpList(denyList);
		this.blockMode = BlockMode.from(blockExternal, blockInternal);
		this.customFilters = new ArrayList<>(customFilters);
	}

	private static List<IpAddressMatcher> initIpList(List<String> ipList) {
		return ipList.stream().map(IpAddressMatcher::new).toList();
	}

	@Override
	public boolean filterAddress(InetAddress address) {
		// A block is final. Check all block conditions first.

		// 1. Block mode
		if (!passBlockMode(address)) {
			return true; // Block
		}

		// 2. Deny list
		if (!passDenyList(address)) {
			return true; // Block
		}

		// 3. Custom filters
		for (InetAddressFilter filter : this.customFilters) {
			if (filter.filterAddress(address)) { // true from custom filter means block
				return true; // Block
			}
		}

		// If an allow list is configured, the address MUST be on it to be allowed.
		// This check is done after block rules, so block rules take precedence.
		if (!this.allowList.isEmpty()) {
			return !passAllowList(address); // Block if it doesn't pass the allow list
		}

		// If we reach here, no rules have blocked the address.
		return false; // Allow
	}

	private boolean passBlockMode(InetAddress address) {
		if (this.blockMode != null) {
			if (this.blockMode == BlockMode.EXTERNAL) {
				return isInternalIp(address);
			}
			if (this.blockMode == BlockMode.INTERNAL) {
				return !isInternalIp(address);
			}
		}
		return true;
	}

	private boolean passAllowList(InetAddress address) {
		if (this.allowList.isEmpty()) {
			return true;
		}
		for (IpAddressMatcher ipAddressMatcher : this.allowList) {
			if (ipAddressMatcher.matches(address)) {
				return true;
			}
		}
		return false;
	}

	private boolean passDenyList(InetAddress address) {
		for (IpAddressMatcher ipAddressMatcher : this.denyList) {
			if (ipAddressMatcher.matches(address)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isInternalIp(InetAddress addr) {

		if (addr.isLoopbackAddress()) {
			return true;
		}

		byte[] rawAddress = addr.getAddress();

		// there is sadly no Stream support for byte arrays
		int[] iAddr = new int[rawAddress.length];
		for (int i = 0; i < rawAddress.length; i++) {
			iAddr[i] = Byte.toUnsignedInt(rawAddress[i]);
		}

		// Ignoring Multicast addresses
		if (addr.getAddress().length == 4) {
			// IPv4 filtering
			// 10.x.x.x , 192.168.x.x , 172.16.x.x
			if (iAddr[0] == 10 || (iAddr[0] == 192 && iAddr[1] == 168) || (iAddr[0] == 172 && iAddr[1] == 16)) {
				return true;
			}

		}
		else if (addr.getAddress().length == 16) {
			// IPv6, check for Unique Local Addresses
			if (iAddr[0] == 0xfc || iAddr[0] == 0xfd) {
				return true;
			}

			// IPv4/IPv6 translation, 64:ff9b
			if (iAddr[0] == 0x00 && iAddr[1] == 0x64 && iAddr[2] == 0xff && iAddr[3] == 0x9b) {
				int[] ipv4Part = new int[] { iAddr[12], iAddr[13], iAddr[14], iAddr[15] };
				// same check as above plus a check for loopback
				if (ipv4Part[0] == 10 || ipv4Part[0] == 127 || (ipv4Part[0] == 192 && ipv4Part[1] == 168)
						|| (ipv4Part[0] == 172 && ipv4Part[1] == 16)) {
					return true;
				}
			}
		}
		return false;
	}

	private enum BlockMode {

		/**
		 * Allow request to the local network only.
		 */
		EXTERNAL,

		/**
		 * Allow requests towards the internet only, e.g. prevent access to cloud VM
		 * metadata.
		 */
		INTERNAL;

		public static @Nullable BlockMode from(boolean blockExternal, boolean blockInternal) {
			return (blockExternal ? EXTERNAL : (blockInternal ? INTERNAL : null));
		}

	}

	/**
	 * Class to represent and IPv4 or IPv6 range to be used in filtering. Inspired by:
	 * org.springframework.security.web.util.matcher.IpAddressMatcher.java
	 */
	private static final class IpAddressMatcher {

		private static final Log logger = LogFactory.getLog(IpAddressMatcher.class);

		private final InetAddress address;

		private final int nMaskBits;

		private IpAddressMatcher(String addressOrRange) {
			if (addressOrRange.indexOf('/') > 0) {
				String[] addressAndMask = addressOrRange.split("/");
				this.address = parseAddress(addressAndMask[0]);
				this.nMaskBits = Integer.parseInt(addressAndMask[1]);
			}
			else {
				this.nMaskBits = -1;
				this.address = parseAddress(addressOrRange);
			}
		}

		private boolean matches(InetAddress toCheck) {

			if (this.nMaskBits < 0) {
				return toCheck.equals(this.address);
			}
			byte[] remAddr = toCheck.getAddress();
			byte[] reqAddr = this.address.getAddress();
			int nMaskFullBytes = this.nMaskBits / 8;
			byte finalByte = (byte) (0xFF00 >> (this.nMaskBits & 0x07));
			for (int i = 0; i < nMaskFullBytes; i++) {
				if (remAddr[i] != reqAddr[i]) {
					return false;
				}
			}
			if (finalByte != 0) {
				return (remAddr[nMaskFullBytes] & finalByte) == (reqAddr[nMaskFullBytes] & finalByte);
			}
			return true;

		}

		private InetAddress parseAddress(String address) {
			try {
				InetAddress result = InetAddress.getByName(address);
				if (address.matches(".*[a-zA-Z\\-].*$") && !address.contains(":")) {
					logger.warn("Hostname '" + address + "' resolved to " + result.toString()
							+ " will be used on IP address matching");
				}
				return result;
			}
			catch (UnknownHostException ex) {
				throw new IllegalArgumentException(String.format("Failed to parse address '%s'", address), ex);
			}
		}

		@Override
		public String toString() {
			return "IpAddressMatcher{" + "address=" + this.address + ", nMaskBits=" + this.nMaskBits + '}';
		}

	}

}
