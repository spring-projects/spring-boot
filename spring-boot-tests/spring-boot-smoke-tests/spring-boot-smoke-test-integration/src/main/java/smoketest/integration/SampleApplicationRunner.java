/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smoketest.integration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * SampleApplicationRunner class.
 */
@Component
public class SampleApplicationRunner implements ApplicationRunner {

	private final SampleMessageGateway gateway;

	/**
	 * Constructs a new SampleApplicationRunner with the specified SampleMessageGateway.
	 * @param gateway the SampleMessageGateway to be used by the SampleApplicationRunner
	 */
	public SampleApplicationRunner(SampleMessageGateway gateway) {
		this.gateway = gateway;
	}

	/**
	 * This method is used to run the application with the given arguments.
	 * @param args The arguments passed to the application.
	 * @throws Exception If an error occurs while running the application.
	 */
	@Override
	public void run(ApplicationArguments args) throws Exception {
		for (String arg : args.getNonOptionArgs()) {
			this.gateway.echo(arg);
		}
	}

}
