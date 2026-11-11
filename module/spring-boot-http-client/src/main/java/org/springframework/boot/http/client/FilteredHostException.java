/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

/**
 * Exception thrown when a host was not used due to an {@link InetAddressFilter}.
 *
 * @author Phillip Webb
 * @since 4.1.0
 */
public class FilteredHostException extends RuntimeException {

	private final String host;

	private final InetAddressFilter filter;

	FilteredHostException(String host, InetAddressFilter filter) {
		super("Filtered host '%s'".formatted(host));
		this.host = host;
		this.filter = filter;
	}

	/**
	 * Return the host that was not matched.
	 * @return the unmatched host
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * Return the filter that was used.
	 * @return the filter that didn't match
	 */
	public InetAddressFilter getFilter() {
		return this.filter;
	}

}
