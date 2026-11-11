/*
 * Copyright 2012-present the original author or authors.
 */

package sample.autoconfig;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWarDeployment;
import org.springframework.context.annotation.Bean;

@ConditionalOnWarDeployment
@AutoConfiguration
public final class ExampleAutoConfiguration {

	@Bean
	TestEndpoint testEndpoint() {
		return new TestEndpoint();
	}

	@Endpoint(id = "war")
	static class TestEndpoint {

		@ReadOperation
		String hello() {
			return "{\"hello\":\"world\"}";
		}

	}

}
