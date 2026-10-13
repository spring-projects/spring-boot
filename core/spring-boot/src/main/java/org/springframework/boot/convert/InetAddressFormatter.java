/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Locale;

import org.springframework.format.Formatter;

/**
 * {@link Formatter} for {@link InetAddress}.
 *
 * @author Phillip Webb
 */
final class InetAddressFormatter implements Formatter<InetAddress> {

	@Override
	public String print(InetAddress object, Locale locale) {
		return object.getHostAddress();
	}

	@Override
	public InetAddress parse(String text, Locale locale) throws ParseException {
		try {
			return InetAddress.getByName(text);
		}
		catch (UnknownHostException ex) {
			throw new IllegalStateException("Unknown host " + text, ex);
		}
	}

}
