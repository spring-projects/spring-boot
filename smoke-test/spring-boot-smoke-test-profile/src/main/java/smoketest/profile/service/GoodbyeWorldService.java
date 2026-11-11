/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.profile.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("goodbye")
public class GoodbyeWorldService implements MessageService {

	@Value("${test.name:World}")
	private String name;

	@Override
	public String getMessage() {
		return "Goodbye " + this.name;
	}

}
