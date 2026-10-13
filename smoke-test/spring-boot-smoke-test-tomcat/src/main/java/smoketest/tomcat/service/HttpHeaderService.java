/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.tomcat.service;

import smoketest.tomcat.util.RandomStringUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpHeaderService {

	@Value("${server.tomcat.max-http-response-header-size}")
	private int maxHttpResponseHeaderSize;

	/**
	 * Generates random data. The data is:
	 * <ol>
	 * <li>is longer than configured
	 * <code>server.tomcat.max-http-response-header-size</code></li>
	 * <li>is url encoded by base 64 encode the random value</li>
	 * </ol>
	 * @return a base64 encoded string of random bytes
	 */
	public String getHeaderValue() {
		return RandomStringUtil.getRandomBase64EncodedString(this.maxHttpResponseHeaderSize + 1);
	}

}
