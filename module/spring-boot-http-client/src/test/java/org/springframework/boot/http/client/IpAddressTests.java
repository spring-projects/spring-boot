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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link IpAddress}.
 *
 * @author Rob Winch
 * @author Phillip Webb
 */
class IpAddressTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void ofWhenAddressIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> IpAddress.of(null))
			.withMessage("'address' must not be empty");
	}

	@Test
	void ofWhenAddressIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> IpAddress.of(""))
			.withMessage("'address' must not be empty");
	}

	@Test
	void ofWithMaskWhenAddressIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> IpAddress.of("192.168.1.1/"))
			.withMessage("'address' subnet mask must be a number");
	}

	@Test
	void ofWhenUnmaskedIpAddress() throws Exception {
		IpAddress address = IpAddress.of("192.168.1.1");
		assertThat(address).extracting("address").isEqualTo(InetAddress.getByName("192.168.1.1"));
		assertThat(address).extracting("maskBitSize").isEqualTo(-1);
	}

	@Test
	void ofWhenMaskedIpAddress() throws Exception {
		IpAddress address = IpAddress.of("192.168.1.1/24");
		assertThat(address).extracting("address").isEqualTo(InetAddress.getByName("192.168.1.1"));
		assertThat(address).extracting("maskBitSize").isEqualTo(24);
	}

	@Test
	void parseInetAddressWhenIpv4() throws Exception {
		InetAddress parsed = IpAddress.parseInetAddress("192.168.1.1");
		assertThat(parsed).isEqualTo(InetAddress.getByName("192.168.1.1"));
	}

	@Test
	void parseInetAddressWhenIpv6InUrl() {
		InetAddress parsed = IpAddress.parseInetAddress("[::1]");
		assertThat(parsed.isLoopbackAddress()).isTrue();
	}

	@Test
	void parseInetAddressWhenIpv6Shortcut() {
		InetAddress parsed = IpAddress.parseInetAddress("::1");
		assertThat(parsed.isLoopbackAddress()).isTrue();
	}

	@Test
	void parseInetAddressWhenLikelyHost() {
		String message = "must be an IP address and not a host name";
		assertThatIllegalArgumentException().isThrownBy(() -> IpAddress.parseInetAddress("https://example.com"))
			.withMessageContaining(message);
		assertThatIllegalArgumentException().isThrownBy(() -> IpAddress.parseInetAddress("192.168.1.2.3"))
			.withMessageContaining(message);
		assertThatIllegalArgumentException()
			.isThrownBy(() -> IpAddress.parseInetAddress("G001:0db8:0000:0000:0000:0000:0000:0000"))
			.withMessageContaining(message);
	}

	@Test
	void parseInetAddressWhenCannotBeParsed() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> IpAddress.parseInetAddress("2001:0db8:0000:0000:0000:0000:0000:000G"))
			.withMessageContaining("must be parsable to an InetAddress");
	}

	@Test
	void ofWithHostnameThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> IpAddress.of("example.com"))
			.withMessage("'address' [example.com] must be an IP address and not a host name");
	}

	@Test
	void matcherWhenUnmaskedIpv4() {
		IpAddress address = IpAddress.of("192.168.1.1");
		assertThatFilter(address).matches("192.168.1.1");
		assertThatFilter(address).doesNotMatch("192.168.1.2");
	}

	@Test
	void matcherWhenUnmaskedIpv6() {
		IpAddress address = IpAddress.of("fe80::21f:5bff:fe33:bd68");
		assertThatFilter(address).matches("fe80::21f:5bff:fe33:bd68");
		assertThatFilter(address).doesNotMatch("fe80::21f:5bff:fe33:bd69");
	}

	@Test
	void matcherWhenMaskedIpv4() {
		IpAddress address = IpAddress.of("192.168.1.0/24");
		assertThatFilter(address).doesNotMatch("192.168.2.1");
		assertThatFilter(address).doesNotMatch("192.168.0.255");
	}

	@Test
	void matcherWhenMaskedWithZero() {
		IpAddress address = IpAddress.of("192.168.1.0/0");
		assertThatFilter(address).matches("192.168.1.1");
		assertThatFilter(address).matches("192.168.1.255");
		assertThatFilter(address).matches("8.8.8.8");
	}

	@Test
	void matcherWhenMaskedIpv6() {
		IpAddress address = IpAddress.of("2001:db8::/48");
		assertThatFilter(address).matches("2001:db8:0:0:0:0:0:0");
		assertThatFilter(address).matches("2001:db8:0:ffff:ffff:ffff:ffff:ffff");
		assertThatFilter(address).doesNotMatch("2001:db8:1:0:0:0:0:0");
	}

	@Test
	void matcherWhenMaskedIpv4OutsideOfByteBoundary() {
		IpAddress address = IpAddress.of("192.168.1.0/30");
		assertThatFilter(address).matches("192.168.1.0");
		assertThatFilter(address).matches("192.168.1.1");
		assertThatFilter(address).matches("192.168.1.2");
		assertThatFilter(address).matches("192.168.1.3");
		assertThatFilter(address).doesNotMatch("192.168.1.4");
	}

	@Test
	void matcherWhenIpv4DoesNotMatchIpv6() {
		assertThatFilter(IpAddress.of("192.168.1.1")).doesNotMatch("fe80::21f:5bff:fe33:bd68");
		assertThatFilter(IpAddress.of("8.8.8.8")).doesNotMatch("0808:0808::");
	}

	@Test
	void matcherWhenIpv6DoesNotMatchIpv4() {
		assertThatFilter(IpAddress.of("fe80::21f:5bff:fe33:bd68")).doesNotMatch("192.168.1.1");
		assertThatFilter(IpAddress.of("0808:0808::/32")).doesNotMatch("8.8.8.8");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void matcherWhenCheckingNullThrowsException() {
		IpAddress address = IpAddress.of("192.168.1.1");
		assertThatIllegalArgumentException().isThrownBy(() -> address.filter().matches((InetAddress) null))
			.withMessage("'address' must not be null");
	}

	@Test
	void addressesInIpRangeMatch() {
		for (int i = 0; i < 255; i++) {
			assertThatFilter(IpAddress.of("192.168.1.0/24")).matches("192.168.1." + i);
		}
		assertThatFilter(IpAddress.of("192.168.1.0/25")).matches("192.168.1.127").doesNotMatch("192.168.1.128");
		assertThatFilter(IpAddress.of("192.168.1.128/25")).matches("192.168.1.255");
		assertThatFilter(IpAddress.of("192.168.1.192/26")).matches("192.168.1.255");
		assertThatFilter(IpAddress.of("192.168.1.224/27")).matches("192.168.1.255");
		assertThatFilter(IpAddress.of("192.168.1.240/27")).matches("192.168.1.255");
		assertThatFilter(IpAddress.of("192.168.1.255/32")).matches("192.168.1.255");
		assertThatFilter(IpAddress.of("202.24.0.0/14")).matches("202.24.199.127");
		assertThatFilter(IpAddress.of("202.24.0.0/14")).matches("202.25.179.135");
		assertThatFilter(IpAddress.of("202.24.0.0/14")).matches("202.26.179.135");
	}

	private static InetAddressFilterAssert assertThatFilter(IpAddress address) {
		return new InetAddressFilterAssert(address.filter());
	}

}
