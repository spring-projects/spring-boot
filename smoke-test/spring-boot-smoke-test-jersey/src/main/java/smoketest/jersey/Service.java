/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Service {

	@Value("${message:World}")
	private String msg;

	public String message() {
		return this.msg;
	}

}
