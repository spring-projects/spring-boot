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
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Strategy interface used for {@link InetAddress}-based filtering.
 * <p>
 * Allow HTTP clients to offer Server-Side Request Forgery (SSRF) mitigation features, for
 * example by only allowing local addresses to be called.
 * <p>
 * Filters are typically built using the static factory methods on this interface,
 * optionally combined with one or more of the logic methods. For example:
 * <pre class="code">
 * InetAddressFilter.of("192.168.0.0/24")
 * 	.andNot("192.168.0.1");
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Phillip Webb
 * @since 4.1.0
 * @see FilteredHostException
 */
@FunctionalInterface
public interface InetAddressFilter {

	/**
	 * Determine whether the given socket address matches.
	 * @param address the socket address string to check
	 * @return if the address matches
	 */
	default boolean matches(InetSocketAddress address) {
		Assert.notNull(address, "'address' must not be null");
		return matches(address.getAddress());
	}

	/**
	 * Whether the given address matches.
	 * @param address the address to check
	 * @return if the address matches
	 */
	boolean matches(InetAddress address);

	/**
	 * Return a composed filter that represents a short-circuiting logical AND of this
	 * filter and other IP addresses.
	 * @param addresses the addresses that will be logically-ANDed with this filter in any
	 * form supported by {@link #of(String...)}
	 * @return a new composed filter instance
	 */
	default InetAddressFilter and(String... addresses) {
		return and(Arrays.stream(addresses).map(IpAddress::of).map(IpAddress::filter).toList());
	}

	/**
	 * Return a composed filter that represents a short-circuiting logical AND of this
	 * filter and other filters.
	 * @param filters the filters that will be logically-ANDed with this filter
	 * @return a new composed filter instance
	 */
	default InetAddressFilter and(InetAddressFilter... filters) {
		return and(Arrays.asList(filters));
	}

	/**
	 * Return a composed filter that represents a short-circuiting logical AND of this
	 * filter and other filters.
	 * @param filters the filters that will be logically-ANDed with this filter
	 * @return a new composed filter instance
	 */
	default InetAddressFilter and(Collection<? extends InetAddressFilter> filters) {
		InetAddressFilter result = this;
		for (InetAddressFilter filter : filters) {
			InetAddressFilter ours = result;
			result = (address) -> ours.matches(address) && filter.matches(address);
		}
		return result;
	}

	/**
	 * Return a composed filter that represents a short-circuiting logical AND of this
	 * filter and other {@link #negate() negated} IP addresses.
	 * @param addresses the addresses that will be {@link #negate() negated} and
	 * logically-ANDed with this filter in any form supported by {@link #of(String...)}
	 * @return a new composed filter instance
	 */
	default InetAddressFilter andNot(String... addresses) {
		return andNot(Arrays.stream(addresses).map(IpAddress::of).map(IpAddress::filter).toList());
	}

	/**
	 * Return a composed filter that represents a short-circuiting logical AND of this
	 * filter and other {@link #negate() negated} filters.
	 * @param filters the filters that will be {@link #negate() negated} and
	 * logically-ANDed with this filter
	 * @return a new composed filter instance
	 */
	default InetAddressFilter andNot(InetAddressFilter... filters) {
		return andNot(Arrays.asList(filters));
	}

	/**
	 * Return a composed filter that represents a short-circuiting logical AND of this
	 * filter and other {@link #negate() negated} filters.
	 * @param filters the filters that will be {@link #negate() negated} and
	 * logically-ANDed with this filter
	 * @return a new composed filter instance
	 */
	default InetAddressFilter andNot(Collection<? extends InetAddressFilter> filters) {
		InetAddressFilter result = this;
		for (InetAddressFilter filter : filters) {
			InetAddressFilter ours = result;
			result = (address) -> ours.matches(address) && !filter.matches(address);
		}
		return result;
	}

	/**
	 * Return a composed filter that represents a short-circuiting logical OR of this
	 * filter and other IP addresses.
	 * @param addresses the addresses that will be logically-ORed with this filter in any
	 * form supported by {@link #of(String...)}
	 * @return a new composed filter instance
	 */
	default InetAddressFilter or(String... addresses) {
		return or(Arrays.stream(addresses).map(IpAddress::of).map(IpAddress::filter).toList());
	}

	/**
	 * Return a composed filter that represents a short-circuiting logical OR of this
	 * filter and other filters.
	 * @param filters the matchers that will be logically-ORed with this filter
	 * @return a new composed filter instance
	 */
	default InetAddressFilter or(InetAddressFilter... filters) {
		return or(Arrays.asList(filters));
	}

	/**
	 * Return a composed filter that represents a short-circuiting logical OR of this
	 * filter and other filters.
	 * @param filters the filters that will be logically-ORed with this filter
	 * @return a new composed filter instance
	 */
	default InetAddressFilter or(Collection<? extends InetAddressFilter> filters) {
		InetAddressFilter result = this;
		for (InetAddressFilter matcher : filters) {
			InetAddressFilter ours = result;
			result = (address) -> ours.matches(address) || matcher.matches(address);
		}
		return result;
	}

	/**
	 * Return a new filter that represents the logical negation of this filter.
	 * @return the negated filter
	 */
	default InetAddressFilter negate() {
		return (address) -> !matches(address);
	}

	/**
	 * Return a filter that will match external (non-private) IP addresses. External
	 * addresses are all non-{@link #internalAddresses() internal addresses}
	 * @return a filter for external IP addresses
	 * @see #internalAddresses()
	 */
	static InetAddressFilter externalAddresses() {
		return routable().andNot(InternalInetAddressFilter.instance);
	}

	/**
	 * Return a filter that will match internal (private) IP addresses.
	 * <p>
	 * Internal addresses include loopback addresses ({@code 127.0.0.0/8} for IPv4,
	 * {@code ::1} for IPv6), private IPv4 address ranges ({@code 10.0.0.0/8},
	 * {@code 172.16.0.0/12}, {@code 192.168.0.0/16}), and IPv6 Unique Local Addresses
	 * ({@code fc00::/7}).
	 * @return a filter for external IP addresses
	 * @see #externalAddresses()
	 */
	static InetAddressFilter internalAddresses() {
		return routable().and(InternalInetAddressFilter.instance);
	}

	/**
	 * Returns a filter that will match all routable addresses (not all zeros).
	 * @return a filter for routable IP addresses
	 */
	static InetAddressFilter routable() {
		return (address) -> {
			Assert.notNull(address, "'address' must not be null");
			byte[] bytes = address.getAddress();
			for (byte b : bytes) {
				if (b != 0) {
					return true;
				}
			}
			return false;
		};
	}

	/**
	 * Return a filter that is the negation of all the given addresses.
	 * @param addresses the addresses to negate in any form supported by
	 * {@link #of(String...)}
	 * @return a negated filter
	 * @see #negate()
	 */
	static InetAddressFilter not(String... addresses) {
		return all().andNot(addresses);
	}

	/**
	 * Return a filter that is the negation of all the given matchers.
	 * @param filters the filters to negate
	 * @return a negated filter
	 * @see #negate()
	 */
	static InetAddressFilter not(InetAddressFilter... filters) {
		return all().andNot(filters);
	}

	/**
	 * Return a filter that is the negation of all the given filters.
	 * @param filters the filters to negate
	 * @return a negated filter
	 * @see #negate()
	 */
	static InetAddressFilter not(Collection<? extends InetAddressFilter> filters) {
		return all().andNot(filters);
	}

	/**
	 * Return a filter that matches any of the given IP addresses. Address may be either a
	 * full IP address (e.g. {@code 192.168.1.1}) or an IP address block spcified using
	 * CIDR notations (for example {@code 192.168.1.0/24}). Both IPv4 and IPv6 addresses
	 * are supported.
	 * @param addresses the IP addresses to match
	 * @return a filter that matches any of the given addresses
	 */
	static InetAddressFilter of(String... addresses) {
		return none().or(addresses);
	}

	/**
	 * Return a filter that matches any of the given filters.
	 * @param filters the filters to include
	 * @return a filter that matches any of the filters
	 */
	static InetAddressFilter of(InetAddressFilter... filters) {
		return none().or(filters);
	}

	/**
	 * Return a filter that matches any of the given filters.
	 * @param filters the filters to include
	 * @return a filter that matches any of the filters
	 */
	static InetAddressFilter of(Collection<? extends InetAddressFilter> filters) {
		return none().or(filters);
	}

	/**
	 * Adapt the given {@link Predicate} into an {@link InetAddressFilter}.
	 * @param predicate the predicate to adapt
	 * @return a filter that matches using the predicate
	 */
	static InetAddressFilter adapt(Predicate<@Nullable InetAddress> predicate) {
		Assert.notNull(predicate, "'predicate' must not be null");
		return (address) -> address != null && predicate.test(address);
	}

	/**
	 * Return a filter that matches all addresses.
	 * @return a filter that matches all
	 */
	static InetAddressFilter all() {
		return (address) -> {
			Assert.notNull(address, "'address' must not be null");
			return true;
		};
	}

	/**
	 * Return a filter that matches no addresses.
	 * @return a filter that matches none
	 */
	static InetAddressFilter none() {
		return (address) -> {
			Assert.notNull(address, "'address' must not be null");
			return false;
		};
	}

}
