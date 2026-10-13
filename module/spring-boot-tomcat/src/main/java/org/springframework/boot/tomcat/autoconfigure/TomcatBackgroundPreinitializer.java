/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.autoconfigure;

import org.apache.catalina.authenticator.NonLoginAuthenticator;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;

import org.springframework.boot.autoconfigure.preinitialize.BackgroundPreinitializer;

/**
 * {@link BackgroundPreinitializer} for Tomcat.
 *
 * @author Phillip Webb
 */
final class TomcatBackgroundPreinitializer implements BackgroundPreinitializer {

	@Override
	public void preinitialize() throws Exception {
		new Rfc6265CookieProcessor();
		new NonLoginAuthenticator();
	}

}
