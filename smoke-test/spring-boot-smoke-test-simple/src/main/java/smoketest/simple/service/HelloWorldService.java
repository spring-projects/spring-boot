/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.simple.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HelloWorldService {

	@Value("${test.name:World}")
	private String name;

	@Value("${test.duration:10s}")
	private Duration duration;

	public String getHelloMessage() {
		return "Hello " + this.name + " for " + this.duration.getSeconds() + " seconds";
	}

}
