/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.tomcat.jsp;

public class MyRestResponse {

	private final String message;

	public MyRestResponse(String message) {
		this.message = message;
	}

	public String getMessage() {
		return this.message;
	}

}
