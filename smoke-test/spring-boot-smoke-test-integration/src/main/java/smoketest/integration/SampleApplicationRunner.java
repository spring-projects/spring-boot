/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.integration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SampleApplicationRunner implements ApplicationRunner {

	private final SampleMessageGateway gateway;

	public SampleApplicationRunner(SampleMessageGateway gateway) {
		this.gateway = gateway;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		for (String arg : args.getNonOptionArgs()) {
			this.gateway.echo(arg);
		}
	}

}
