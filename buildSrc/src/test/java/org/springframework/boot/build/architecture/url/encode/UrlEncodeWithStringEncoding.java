/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.url.encode;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

class UrlEncodeWithStringEncoding {

	void exampleMethod() throws UnsupportedEncodingException {
		URLEncoder.encode("https://example.com", "UTF-8");
	}

}
