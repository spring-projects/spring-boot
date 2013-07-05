/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.zero.sample.profile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.zero.CommandLineRunner;
import org.springframework.zero.SpringApplication;
import org.springframework.zero.context.annotation.EnableAutoConfiguration;
import org.springframework.zero.sample.profile.service.MessageService;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class SampleProfileApplication implements CommandLineRunner {

	// Simple example shows how a command line spring application can execute an
	// injected bean service. Also demonstrates how you can use @Value to inject
	// command line args ('--name=whatever') or application properties

	@Autowired
	private MessageService helloWorldService;

	@Override
	public void run(String... args) {
		System.out.println(this.helloWorldService.getMessage());
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleProfileApplication.class, args);
	}
}
