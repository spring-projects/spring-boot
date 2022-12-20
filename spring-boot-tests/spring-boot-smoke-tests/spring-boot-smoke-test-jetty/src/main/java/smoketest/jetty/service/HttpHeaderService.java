package smoketest.jetty.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import smoketest.jetty.util.RandomStringUtil;

@Component
public class HttpHeaderService {

	@Value("${server.jetty.max-http-response-header-size}")
	private int maxHttpResponseHeaderSize;

	/**
	 * generate a random byte array that
	 * <ol>
	 * <li>is longer than configured
	 * <code>server.jetty.max-http-response-header-size</code></li>
	 * <li>is url encoded by base 64 encode the random value</li>
	 * </ol>
	 * @return a base64 encoded string of random bytes
	 */
	public String getHeaderValue() {
		return RandomStringUtil.getRandomBase64EncodedString(maxHttpResponseHeaderSize + 1);
	}

}
