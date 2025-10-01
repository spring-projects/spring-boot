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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Component to filter addresses according configured security rules. For use from an HTTP
 * client's DNS resolver.
 *
 * <p>
 * Use {@link #builder()} to create an instance.
 *
 * @author Scott Frederick
 * @since 3.2.0
 */
public final class SecurityDnsHandler {

	private static final Log logger = LogFactory.getLog(SecurityDnsHandler.class);

	private final InetAddressFilter inetAddressFilter;

	private final boolean reportOnly;

	public SecurityDnsHandler(InetAddressFilter filter, boolean reportOnly) {
		this.inetAddressFilter = filter;
		this.reportOnly = reportOnly;
	}

	public boolean getReportMode() {
		return this.reportOnly;
	}

	public List<InetAddress> handleAddresses(List<InetAddress> candidateAddresses) {
		List<InetAddress> blocked = null;
		for (InetAddress address : candidateAddresses) {
			if (this.inetAddressFilter.filterAddress(address)) {
				blocked = (blocked != null) ? blocked : new ArrayList<>();
				blocked.add(address);
			}
		}

		if (blocked == null) {
			return candidateAddresses;
		}

		if (logger.isErrorEnabled()) {
			logger.error("Blocked IP addresses: " + candidateAddresses);
		}

		if (this.reportOnly) {
			return candidateAddresses;
		}

		ArrayList<InetAddress> result = new ArrayList<>(candidateAddresses);
		result.removeAll(blocked);
		return result;
	}

	public List<InetSocketAddress> handleInetSocketAddresses(List<InetSocketAddress> candidates) {
		if (candidates == null || candidates.isEmpty()) {
			return candidates;
		}
		List<InetAddress> input = candidates.stream().map((isa) -> isa.getAddress()).distinct().toList();
		List<InetAddress> output = handleAddresses(input);
		// Use the original port for each address
		return candidates.stream().filter((isa) -> output.contains(isa.getAddress())).toList();
	}

	public List<InetSocketAddress> handleInetSocketAddresses(List<InetSocketAddress> candidates, int port) {
		List<InetAddress> input = candidates.stream().map(InetSocketAddress::getAddress).distinct().toList();
		List<InetAddress> output = handleAddresses(input);
		return output.stream().map((address) -> new InetSocketAddress(address, port)).toList();
	}

	public List<SocketAddress> handleSocketAddresses(List<? extends SocketAddress> candidates) {
		if (candidates == null || candidates.isEmpty()) {
			return new ArrayList<>(candidates);
		}
		// Extract InetSocketAddress instances
		List<InetSocketAddress> inetCandidates = candidates.stream()
			.filter(InetSocketAddress.class::isInstance)
			.map(InetSocketAddress.class::cast)
			.toList();

		List<InetSocketAddress> filteredInet = handleInetSocketAddresses(inetCandidates);

		// Only keep those InetSocketAddress that passed the filter, and preserve order
		return new ArrayList<SocketAddress>(candidates.stream()
			.filter((sa) -> !(sa instanceof InetSocketAddress) || filteredInet.contains(sa))
			.toList());
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final List<String> allowList = new ArrayList<>();

		private final List<String> denyList = new ArrayList<>();

		private boolean blockAllExternal;

		private boolean blockAllInternal;

		private final List<InetAddressFilter> customFilters = new ArrayList<>();

		private boolean reportOnly;

		public Builder blockAllExternal(boolean block) {
			this.blockAllExternal = block;
			return this;
		}

		public Builder blockAllInternal(boolean block) {
			this.blockAllInternal = block;
			return this;
		}

		public Builder allowList(String... ipAddresses) {
			this.allowList.addAll(Arrays.asList(ipAddresses));
			return this;
		}

		public Builder denyList(String... ipAddresses) {
			this.denyList.addAll(Arrays.asList(ipAddresses));
			return this;
		}

		public Builder customFilter(InetAddressFilter filter) {
			this.customFilters.add(filter);
			return this;
		}

		public Builder reportOnly(boolean report) {
			this.reportOnly = report;
			return this;
		}

		public SecurityDnsHandler build() {

			DefaultInetAddressFilter filter = new DefaultInetAddressFilter(this.allowList, this.denyList,
					this.blockAllExternal, this.blockAllInternal, this.customFilters);

			return new SecurityDnsHandler(filter, this.reportOnly);
		}

	}

}
