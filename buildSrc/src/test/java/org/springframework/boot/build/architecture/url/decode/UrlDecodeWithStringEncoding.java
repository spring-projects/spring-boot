/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.url.decode;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

class UrlDecodeWithStringEncoding {

	void exampleMethod() throws UnsupportedEncodingException {
		URLDecoder.decode("https://example.com", "UTF-8");
	}

}
