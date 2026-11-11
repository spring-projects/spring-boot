/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jetty.service;

import smoketest.jetty.util.StringUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpHeaderService {

	@Value("${server.jetty.max-http-response-header-size}")
	private int maxHttpResponseHeaderSize;

	/**
	 * Generates a header value, which is longer than
	 * 'server.jetty.max-http-response-header-size'.
	 * @return the header value
	 */
	public String getHeaderValue() {
		return StringUtil.repeat('A', this.maxHttpResponseHeaderSize + 1);
	}

}
