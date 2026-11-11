/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.assertj.core.api.AbstractObjectAssert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assertions for an {@link InetAddressFilter}.
 *
 * @author Phillip Webb
 */
class InetAddressFilterAssert extends AbstractObjectAssert<InetAddressFilterAssert, InetAddressFilter> {

	InetAddressFilterAssert(InetAddressFilter actual) {
		super(actual, InetAddressFilterAssert.class);
	}

	InetAddressFilterAssert matches(String address) {
		return matches(asInetAddress(address));
	}

	InetAddressFilterAssert matches(InetAddress address) {
		isNotNull();
		assertThat(this.actual.matches(address)).as("Matches address %s", address).isTrue();
		return this;
	}

	InetAddressFilterAssert matches(InetSocketAddress address) {
		isNotNull();
		assertThat(this.actual.matches(address)).as("Matches socket address %s", address).isTrue();
		return this;
	}

	InetAddressFilterAssert doesNotMatch(String address) {
		return doesNotMatch(asInetAddress(address));
	}

	InetAddressFilterAssert doesNotMatch(InetAddress address) {
		isNotNull();
		assertThat(this.actual.matches(address)).as("Does not match address %s", address).isFalse();
		return this;
	}

	InetAddressFilterAssert doesNotMatch(InetSocketAddress address) {
		isNotNull();
		assertThat(this.actual.matches(address)).as("Does not match socket address %s", address).isFalse();
		return this;
	}

	private InetAddress asInetAddress(String address) {
		try {
			return InetAddress.getByName(address);
		}
		catch (UnknownHostException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
