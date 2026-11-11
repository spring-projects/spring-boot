/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.test.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNameNotFoundException extends RuntimeException {

	private final String username;

	public UserNameNotFoundException(String username) {
		this.username = username;
	}

	public String getUsername() {
		return this.username;
	}

}
