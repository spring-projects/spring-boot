/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.websocket.jetty.echo;

import org.jspecify.annotations.Nullable;

public class DefaultEchoService implements EchoService {

	private final String echoFormat;

	public DefaultEchoService(@Nullable String echoFormat) {
		this.echoFormat = (echoFormat != null) ? echoFormat : "%s";
	}

	@Override
	public String getMessage(String message) {
		return String.format(this.echoFormat, message);
	}

}
