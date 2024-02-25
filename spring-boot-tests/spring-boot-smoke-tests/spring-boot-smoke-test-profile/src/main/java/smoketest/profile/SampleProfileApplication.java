/*
 * Copyright 2012-2022 the original author or authors.
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

package smoketest.profile;

import smoketest.profile.service.MessageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * SampleProfileApplication class.
 */
@SpringBootApplication
public class SampleProfileApplication implements CommandLineRunner {

	// Simple example shows how a command line spring application can execute an
	// injected bean service. Also demonstrates how you can use @Value to inject
	// command line args ('--test.name=whatever') or application properties

	@Autowired
	private MessageService helloWorldService;

	/**
     * This method is the entry point of the application and is responsible for printing the message
     * returned by the helloWorldService.
     *
     * @param args The command line arguments passed to the application.
     */
    @Override
	public void run(String... args) {
		System.out.println(this.helloWorldService.getMessage());
	}

	/**
     * The main method of the SampleProfileApplication class.
     * 
     * @param args The command line arguments passed to the application.
     */
    public static void main(String... args) {
		SpringApplication application = new SpringApplication(SampleProfileApplication.class) {

			@Override
			protected void bindToSpringApplication(ConfigurableEnvironment environment) {
			}

		};
		application.setWebApplicationType(WebApplicationType.NONE);
		application.run(args);
	}

}
