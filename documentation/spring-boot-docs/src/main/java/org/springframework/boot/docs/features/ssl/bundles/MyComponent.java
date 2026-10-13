/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.ssl.bundles;

import javax.net.ssl.SSLContext;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.stereotype.Component;

@Component
public class MyComponent {

	@SuppressWarnings("unused")
	public MyComponent(SslBundles sslBundles) {
		SslBundle sslBundle = sslBundles.getBundle("mybundle");
		SSLContext sslContext = sslBundle.createSslContext();
		// do something with the created sslContext
	}

}
