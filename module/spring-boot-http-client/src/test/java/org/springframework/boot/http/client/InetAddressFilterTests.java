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
import java.net.InetSocketAddress;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.security.util.matcher.InetAddressMatcher;
import org.springframework.security.util.matcher.InetAddressMatchers;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link InetAddressFilter} and {@link InternalInetAddressFilter}.
 *
 * @author Rob Winch
 * @author Phillip Webb
 */
class InetAddressFilterTests {

	private static InetAddressFilterAssert assertThat(InetAddressFilter filter) {
		return new InetAddressFilterAssert(filter);
	}

	@Nested
	class MatchesSocketAddressTests {

		@Test
		void whenIpv4() {
			InetAddressFilter filter = InetAddressFilter.of("192.168.1.1");
			assertThat(filter).matches(new InetSocketAddress("192.168.1.1", 8080));
			assertThat(filter).doesNotMatch(new InetSocketAddress("192.168.1.2", 8080));
		}

		@Test
		void whenIpv6() {
			InetAddressFilter filter = InetAddressFilter.of("fe80:0:0:0:21f:5bff:fe33:bd68");
			assertThat(filter).matches(new InetSocketAddress("fe80::21f:5bff:fe33:bd68", 8080));
			assertThat(filter).doesNotMatch(new InetSocketAddress("fe90::21f:5bff:fe33:bd68", 8080));
		}

		@Test
		@SuppressWarnings("NullAway") // Test null check
		void whenNull() {
			InetAddressFilter filter = (address) -> address != null;
			assertThatIllegalArgumentException().isThrownBy(() -> filter.matches((InetSocketAddress) null))
				.withMessage("'address' must not be null");
		}

		@Test
		void whenLambda() {
			InetAddressFilter filter = (address) -> address.getHostAddress().startsWith("192.168");
			assertThat(filter).matches(new InetSocketAddress("192.168.1.1", 8080));
			assertThat(filter).matches(new InetSocketAddress("192.168.100.200", 8080));
			assertThat(filter).doesNotMatch(new InetSocketAddress("10.0.0.1", 8080));
		}

	}

	@Nested
	class AndTests {

		@Test
		void stringsWhenEmpty() {
			InetAddressFilter originalFilter = (address) -> true;
			InetAddressFilter filter = originalFilter.and(new String[] {});
			assertThat(filter).isSameAs(originalFilter);
		}

		@Test
		void stringsWhenSingle() {
			InetAddressFilter originalFilter = InetAddressFilter.of("192.168.1.1/24");
			InetAddressFilter filter = originalFilter.and("192.168.1.1");
			assertThat(originalFilter).matches("192.168.1.1");
			assertThat(originalFilter).matches("192.168.1.2");
			assertThat(filter).matches("192.168.1.1");
			assertThat(filter).doesNotMatch("192.168.1.2");
		}

		@Test
		void stringsWhenMultiple() {
			InetAddressFilter originalFilter = InetAddressFilter.of("192.168.1.1/16");
			InetAddressFilter filter = originalFilter.and("192.168.1.1/24", "192.168.1.1");
			assertThat(originalFilter).matches("192.168.1.1");
			assertThat(originalFilter).matches("192.168.1.2");
			assertThat(originalFilter).matches("192.168.2.1");
			assertThat(filter).matches("192.168.1.1");
			assertThat(filter).doesNotMatch("192.168.1.2");
			assertThat(filter).doesNotMatch("192.168.2.1");
		}

		@Test
		void filters() {
			InetAddressFilter startsWithTen = (address) -> address.getHostAddress().startsWith("10.");
			InetAddressFilter endsWithOne = (address) -> address.getHostAddress().endsWith(".1");
			InetAddressFilter filter = startsWithTen.and(endsWithOne);
			assertThat(filter).matches("10.0.0.1");
			assertThat(filter).doesNotMatch("10.0.0.2");
			assertThat(filter).doesNotMatch("192.168.1.1");
		}

		@Test
		void collection() {
			InetAddressFilter originalfilter = InetAddressFilter.of("192.168.1.1/24");
			InetAddressFilter filter = originalfilter.and(List.of(InetAddressFilter.of("192.168.1.1")));
			assertThat(originalfilter).matches("192.168.1.1");
			assertThat(originalfilter).matches("192.168.1.2");
			assertThat(filter).matches("192.168.1.1");
			assertThat(filter).doesNotMatch("192.168.1.2");
		}

	}

	@Nested
	class AndNotTests {

		@Test
		void stringsWhenEmpty() {
			InetAddressFilter originalFilter = (address) -> true;
			InetAddressFilter filter = originalFilter.andNot(new String[] {});
			assertThat(filter).isSameAs(originalFilter);
		}

		@Test
		void stringsWhenSingle() {
			InetAddressFilter originalFilter = InetAddressFilter.of("192.168.1.1/24");
			InetAddressFilter filter = originalFilter.andNot("192.168.1.1");
			assertThat(originalFilter).matches("192.168.1.1");
			assertThat(originalFilter).matches("192.168.1.2");
			assertThat(filter).doesNotMatch("192.168.1.1");
			assertThat(filter).matches("192.168.1.2");
		}

		@Test
		void stringsWhenMultiple() {
			InetAddressFilter originalFilter = InetAddressFilter.of("192.168.1.1/24");
			InetAddressFilter filter = originalFilter.andNot("192.168.1.1", "192.168.1.2");
			assertThat(originalFilter).matches("192.168.1.1");
			assertThat(originalFilter).matches("192.168.1.2");
			assertThat(originalFilter).matches("192.168.1.3");
			assertThat(filter).doesNotMatch("192.168.1.1");
			assertThat(filter).doesNotMatch("192.168.1.2");
			assertThat(filter).matches("192.168.1.3");
		}

		@Test
		void filters() {
			InetAddressFilter startsWithTen = (address) -> address.getHostAddress().startsWith("10.");
			InetAddressFilter endsWithOne = (address) -> address.getHostAddress().endsWith(".1");
			InetAddressFilter filter = startsWithTen.andNot(endsWithOne);
			assertThat(filter).doesNotMatch("10.0.0.1");
			assertThat(filter).matches("10.0.0.2");
			assertThat(filter).doesNotMatch("192.168.1.1");
		}

		@Test
		void collection() {
			InetAddressFilter originalFilter = InetAddressFilter.of("192.168.1.1/24");
			InetAddressFilter filter = originalFilter.andNot(List.of(InetAddressFilter.of("192.168.1.1")));
			assertThat(originalFilter).matches("192.168.1.1");
			assertThat(originalFilter).matches("192.168.1.2");
			assertThat(filter).doesNotMatch("192.168.1.1");
			assertThat(filter).matches("192.168.1.2");
		}

	}

	@Nested
	class OrTests {

		@Test
		void stringsWhenEmpty() {
			InetAddressFilter originalFilter = (address) -> true;
			InetAddressFilter filter = originalFilter.or(new String[] {});
			assertThat(filter).isSameAs(originalFilter);
		}

		@Test
		void stringsWhenSingle() {
			InetAddressFilter originalFilter = InetAddressFilter.of("192.168.1.1");
			InetAddressFilter filter = originalFilter.or("192.168.1.2");
			assertThat(originalFilter).matches("192.168.1.1");
			assertThat(originalFilter).doesNotMatch("192.168.1.2");
			assertThat(filter).matches("192.168.1.1");
			assertThat(filter).matches("192.168.1.2");
		}

		@Test
		void stringsWhenMultiple() {
			InetAddressFilter originalFilter = InetAddressFilter.of("192.168.1.1");
			InetAddressFilter filter = originalFilter.or("192.168.1.2", "192.168.1.3");
			assertThat(originalFilter).matches("192.168.1.1");
			assertThat(originalFilter).doesNotMatch("192.168.1.2");
			assertThat(originalFilter).doesNotMatch("192.168.1.3");
			assertThat(filter).matches("192.168.1.1");
			assertThat(filter).matches("192.168.1.2");
			assertThat(filter).matches("192.168.1.3");
		}

		@Test
		void filter() {
			InetAddressFilter originalFilter = InetAddressFilter.of("192.168.1.1");
			InetAddressFilter filter = originalFilter.or(InetAddressFilter.of("192.168.1.2"));
			assertThat(originalFilter).matches("192.168.1.1");
			assertThat(originalFilter).doesNotMatch("192.168.1.2");
			assertThat(filter).matches("192.168.1.1");
			assertThat(filter).matches("192.168.1.2");
		}

		@Test
		void collection() {
			InetAddressFilter originalFilter = InetAddressFilter.of("192.168.1.1");
			InetAddressFilter filter = originalFilter.or(List.of(InetAddressFilter.of("192.168.1.2")));
			assertThat(originalFilter).matches("192.168.1.1");
			assertThat(originalFilter).doesNotMatch("192.168.1.2");
			assertThat(filter).matches("192.168.1.1");
			assertThat(filter).matches("192.168.1.2");
		}

	}

	@Nested
	class NegateTests {

		@Test
		void negate() {
			InetAddressFilter originalFilter = InetAddressFilter.of("192.168.1.1");
			InetAddressFilter filter = originalFilter.negate();
			assertThat(originalFilter).matches("192.168.1.1");
			assertThat(originalFilter).doesNotMatch("192.168.1.2");
			assertThat(filter).doesNotMatch("192.168.1.1");
			assertThat(filter).matches("192.168.1.2");
		}

	}

	@Nested
	class ExternalAddressesTests {

		@Test
		@SuppressWarnings("NullAway") // Test null check
		void nullInetAddress() {
			InetAddressFilter filter = InetAddressFilter.externalAddresses();
			assertThatIllegalArgumentException().isThrownBy(() -> filter.matches((InetAddress) null))
				.withMessage("'address' must not be null");
		}

		@Test
		void ipv4Public() {
			InetAddressFilter filter = InetAddressFilter.externalAddresses();
			assertThat(filter).matches("8.8.8.8");
			assertThat(filter).matches("1.1.1.1");
		}

		@Test
		void ipv6Public() {
			InetAddressFilter filter = InetAddressFilter.externalAddresses();
			assertThat(filter).matches("2001:4860:4860::8888");
		}

		@Test
		void ipv4Private() {
			InetAddressFilter filter = InetAddressFilter.externalAddresses();
			assertThat(filter).doesNotMatch("192.168.1.1");
			assertThat(filter).doesNotMatch("10.0.0.1");
			assertThat(filter).doesNotMatch("172.16.0.1");
		}

		@Test
		void ipv4Loopback() {
			InetAddressFilter filter = InetAddressFilter.externalAddresses();
			assertThat(filter).doesNotMatch("127.0.0.1");
			assertThat(filter).doesNotMatch("127.1.1.1");
		}

		@Test
		void ipv4LinkLocal() {
			InetAddressFilter filter = InetAddressFilter.externalAddresses();
			assertThat(filter).doesNotMatch("169.254.0.0");
			assertThat(filter).doesNotMatch("169.254.169.254");
			assertThat(filter).doesNotMatch("169.254.255.255");
		}

		@Test
		void ipv6Loopback() {
			InetAddressFilter filter = InetAddressFilter.externalAddresses();
			assertThat(filter).doesNotMatch("::1");
			assertThat(filter).doesNotMatch("0000::1");
		}

		@Test
		void ipv6UniqueLocal() {
			InetAddressFilter filter = InetAddressFilter.externalAddresses();
			assertThat(filter).doesNotMatch("fc00::1");
			assertThat(filter).doesNotMatch("fd00::1");
		}

		@Test
		void ipv4NonRoutable() {
			InetAddressFilter filter = InetAddressFilter.externalAddresses();
			assertThat(filter).doesNotMatch("0.0.0.0");
		}

		@Test
		void ipv6NonRoutable() {
			InetAddressFilter filter = InetAddressFilter.externalAddresses();
			assertThat(filter).doesNotMatch("0000:0000:0000:0000:0000:0000:0000:0000");
			assertThat(filter).doesNotMatch("::");
		}

	}

	@Nested
	class InternalAddresses {

		@Test
		@SuppressWarnings("NullAway") // Test null check
		void nullInetAddress() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThatIllegalArgumentException().isThrownBy(() -> filter.matches((InetAddress) null))
				.withMessage("'address' must not be null");
		}

		@Test
		void ipv4Loopback() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).matches("127.0.0.1");
			assertThat(filter).matches("127.1.1.1");
			assertThat(filter).matches("127.0.0.255");
		}

		@Test
		void ipv6Loopback() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).matches("::1");
			assertThat(filter).matches("0000::1");
		}

		@Test
		void ipv4PrivateClass10() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).matches("10.0.0.1");
			assertThat(filter).matches("10.255.255.255");
		}

		@Test
		void ipv4PrivateClass192() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).matches("192.168.0.1");
			assertThat(filter).matches("192.168.255.255");
		}

		@Test
		void ipv4LinkLocal() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).matches("169.254.0.0");
			assertThat(filter).matches("169.254.169.254");
			assertThat(filter).matches("169.254.255.255");
		}

		@Test
		void ipv4PrivateClass172() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).matches("172.16.0.1");
			assertThat(filter).matches("172.16.255.255");
			assertThat(filter).matches("172.17.1.1");
			assertThat(filter).matches("172.31.255.255");
		}

		@Test
		void ipv4MappedIpv6Internal() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).matches("::ffff:127.0.0.1");
			assertThat(filter).matches("::ffff:192.168.1.1");
			assertThat(filter).matches("::ffff:169.254.169.254");
			assertThat(filter).matches("::ffff:10.0.0.1");
		}

		@Test
		void ipv6UniqueLocal() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).matches("fc00::1");
			assertThat(filter).matches("fd00::1");
		}

		@Test
		void ipv6TranslationWithInternalIpv4() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).matches("64:ff9b::10.0.0.1");
			assertThat(filter).matches("64:ff9b::127.0.0.1");
			assertThat(filter).matches("64:ff9b::192.168.1.1");
			assertThat(filter).matches("64:ff9b::172.16.0.1");
		}

		@Test
		void ipv6TranslationWithIpv4StartsWith192ButNot168() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).doesNotMatch("64:ff9b::192.0.2.1");
			assertThat(filter).doesNotMatch("64:ff9b::192.167.1.1");
		}

		@Test
		void ipv6TranslationWithIpv4StartsWith172And16() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).matches("64:ff9b::172.16.0.1");
			assertThat(filter).matches("64:ff9b::172.16.255.255");
		}

		@Test
		@ValueSource(strings = {})
		void ipv6TranslationWithExternalIpv4() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).doesNotMatch("64:ff9b::8.8.8.8");
			assertThat(filter).doesNotMatch("64:ff9b::1.1.1.1");
		}

		@Test
		void ppv6NonTranslationPrefixByte0() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).doesNotMatch("65:ff9b::10.0.0.1");
		}

		@Test
		void ipv6NonTranslationPrefixByte1() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).doesNotMatch("64:fe9b::10.0.0.1");
		}

		@Test
		void ipv6NonTranslationPrefixByte2() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).doesNotMatch("64:ff9a::10.0.0.1");
		}

		@Test
		void ipv6NonTranslationPrefixByte3() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).doesNotMatch("64:ff9c::10.0.0.1");
		}

		@Test
		void ipv4Public() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).doesNotMatch("8.8.8.8");
			assertThat(filter).doesNotMatch("1.1.1.1");
		}

		@Test
		void ipv4StartsWith192ButNot168() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).doesNotMatch("192.0.2.1");
			assertThat(filter).doesNotMatch("192.167.1.1");
			assertThat(filter).doesNotMatch("192.169.1.1");
		}

		@Test
		void ipv4StartsWith172ButNotPrivate16To31() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).doesNotMatch("172.15.1.1");
			assertThat(filter).doesNotMatch("172.32.1.1");
		}

		@Test
		void ipv6Public() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).doesNotMatch("2001:4860:4860::8888");
		}

		@Test
		void ipv4NonRoutable() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).doesNotMatch("0.0.0.0");
		}

		@Test
		void ipv6NonRoutable() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses();
			assertThat(filter).doesNotMatch("0000:0000:0000:0000:0000:0000:0000:0000");
			assertThat(filter).doesNotMatch("::");
		}

	}

	@Nested
	class Routable {

		@Test
		void nonRoutable() {
			InetAddressFilter filter = InetAddressFilter.routable();
			assertThat(filter).doesNotMatch("0.0.0.0");
			assertThat(filter).doesNotMatch("0000:0000:0000:0000:0000:0000:0000:0000");
			assertThat(filter).doesNotMatch("::");

		}

		@Test
		void routable() {
			InetAddressFilter filter = InetAddressFilter.routable();
			assertThat(filter).matches("0.0.0.1");
			assertThat(filter).matches("0000:0000:0000:0000:0000:0000:0000:0001");
		}

	}

	@Nested
	class NotTests {

		@Test
		void stringsWhenEmpty() {
			InetAddressFilter filter = InetAddressFilter.not(new String[] {});
			assertThat(filter).matches("192.168.1.1");
			assertThat(filter).matches("8.8.8.8");
		}

		@Test
		void stringsWhenSingle() {
			InetAddressFilter filter = InetAddressFilter.not("192.168.1.1");
			assertThat(filter).doesNotMatch("192.168.1.1");
			assertThat(filter).matches("192.168.1.2");
		}

		@Test
		void stringsWhenMultiple() {
			InetAddressFilter filter = InetAddressFilter.not("192.168.1.1", "10.0.0.1");
			assertThat(filter).doesNotMatch("192.168.1.1");
			assertThat(filter).doesNotMatch("10.0.0.1");
			assertThat(filter).matches("8.8.8.8");
		}

		@Test
		void stringsWhenCidr() {
			InetAddressFilter filter = InetAddressFilter.not("192.168.1.0/24");
			assertThat(filter).matches("192.168.2.1");
			assertThat(filter).doesNotMatch("192.168.1.1");
			assertThat(filter).doesNotMatch("192.168.1.255");
		}

		@Test
		void filters() {
			InetAddressFilter filter = InetAddressFilter.not(InetAddressFilter.of("192.168.1.1"));
			assertThat(filter).doesNotMatch("192.168.1.1");
			assertThat(filter).matches("192.168.1.2");
		}

		@Test
		void collection() {
			InetAddressFilter filter = InetAddressFilter.not(List.of(InetAddressFilter.of("192.168.1.1")));
			assertThat(filter).doesNotMatch("192.168.1.1");
			assertThat(filter).matches("192.168.1.2");
		}

	}

	@Nested
	class OfTests {

		@Test
		void stringsWhenEmpty() {
			InetAddressFilter filter = InetAddressFilter.of(new String[] {});
			assertThat(filter).doesNotMatch("192.168.1.1");
			assertThat(filter).doesNotMatch("8.8.8.8");
		}

		@Test
		void stringsWhenSingle() {
			InetAddressFilter filter = InetAddressFilter.of("192.168.1.1");
			assertThat(filter).matches("192.168.1.1");
			assertThat(filter).doesNotMatch("192.168.1.2");
		}

		@Test
		void stringsWhenMultiple() {
			InetAddressFilter filter = InetAddressFilter.of("192.168.1.1", "10.0.0.1");
			assertThat(filter).matches("192.168.1.1");
			assertThat(filter).matches("10.0.0.1");
			assertThat(filter).doesNotMatch("8.8.8.8");
		}

		@Test
		void stringsWhenCidr() {
			InetAddressFilter filter = InetAddressFilter.of("192.168.1.0/24");
			assertThat(filter).matches("192.168.1.1");
			assertThat(filter).matches("192.168.1.255");
			assertThat(filter).doesNotMatch("192.168.2.1");
		}

		@Test
		void filter() {
			InetAddressFilter originalfilter = (address) -> address.getHostAddress().startsWith("10.");
			InetAddressFilter filter = InetAddressFilter.of(originalfilter);
			assertThat(filter).matches("10.0.0.1");
			assertThat(filter).doesNotMatch("192.168.1.1");
		}

		@Test
		void collection() {
			InetAddressFilter originalfilter = (address) -> address.getHostAddress().startsWith("10.");
			InetAddressFilter filter = InetAddressFilter.of(List.of(originalfilter));
			assertThat(filter).matches("10.0.0.1");
			assertThat(filter).doesNotMatch("192.168.1.1");
		}

	}

	@Nested
	class CompositeTests {

		@Test
		void ofAndNot() {
			InetAddressFilter filter = InetAddressFilter.of("192.168.1.0/24").andNot("192.168.1.100");
			assertThat(filter).matches("192.168.1.1");
			assertThat(filter).doesNotMatch("192.168.1.100");
			assertThat(filter).doesNotMatch("192.168.2.1");
		}

		@Test
		void ofOr() {
			InetAddressFilter filter = InetAddressFilter.of("192.168.1.100").or("192.168.1.101");
			assertThat(filter).matches("192.168.1.100");
			assertThat(filter).matches("192.168.1.101");
			assertThat(filter).doesNotMatch("192.168.1.102");
		}

		@Test
		void ofAnd() {
			InetAddressFilter filter = InetAddressFilter.of("192.168.1.0/24")
				.and((address) -> address.getHostAddress().endsWith(".1"));
			assertThat(filter).matches("192.168.1.1");
			assertThat(filter).doesNotMatch("192.168.1.2");
		}

		@Test
		void ofInternalAddressOrAndNot() {
			InetAddressFilter filter = InetAddressFilter.internalAddresses()
				.or("8.8.8.8", "8.8.4.4")
				.andNot("192.168.2.0/24");
			assertThat(filter).matches("192.168.1.1");
			assertThat(filter).matches("8.8.8.8");
			assertThat(filter).matches("8.8.4.4");
			assertThat(filter).doesNotMatch("192.168.2.1");
		}

	}

	@Nested
	class AllTests {

		@Test
		void all() {
			InetAddressFilter filter = InetAddressFilter.all();
			assertThat(filter).matches("192.168.1.1");
			assertThat(filter).matches("8.8.8.8");
		}

		@Test
		@SuppressWarnings("NullAway") // Test null check
		void allWhenNull() {
			InetAddressFilter filter = InetAddressFilter.all();
			assertThatIllegalArgumentException().isThrownBy(() -> filter.matches((InetAddress) null))
				.withMessage("'address' must not be null");
		}

	}

	@Nested
	class NoneTests {

		@Test
		void none() {
			InetAddressFilter filter = InetAddressFilter.none();
			assertThat(filter).doesNotMatch("192.168.1.1");
			assertThat(filter).doesNotMatch("8.8.8.8");
		}

		@Test
		@SuppressWarnings("NullAway") // Test null check
		void noneWhenNull() {
			InetAddressFilter filter = InetAddressFilter.none();
			assertThatIllegalArgumentException().isThrownBy(() -> filter.matches((InetAddress) null))
				.withMessage("'address' must not be null");
		}

	}

	@Nested
	class AdaptTests {

		@Test
		void adaptsSpringSecurity() {
			InetAddressMatcher securityMatcher = InetAddressMatchers.matchInternal().build();
			InetAddressFilter filter = InetAddressFilter.adapt(securityMatcher::matches);
			assertThat(filter).matches("127.0.0.1");
			assertThat(filter).doesNotMatch("8.8.8.8");
			assertThatFilterDoesNotMatchNull(filter);
		}

		@SuppressWarnings("NullAway") // Test null check
		private void assertThatFilterDoesNotMatchNull(InetAddressFilter filter) {
			assertThat(filter).doesNotMatch((InetAddress) null);
		}

	}

}
