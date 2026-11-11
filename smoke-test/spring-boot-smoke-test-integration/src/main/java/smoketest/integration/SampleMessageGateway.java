/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.integration;

import org.springframework.integration.annotation.MessagingGateway;

@MessagingGateway(defaultRequestChannel = "outputChannel")
public interface SampleMessageGateway {

	void echo(String message);

}
