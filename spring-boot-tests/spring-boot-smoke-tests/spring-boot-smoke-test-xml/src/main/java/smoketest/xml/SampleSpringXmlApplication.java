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

package smoketest.xml;

import java.util.Collections;

import smoketest.xml.service.HelloWorldService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SampleSpringXmlApplication class.
 */
@SpringBootApplication
public class SampleSpringXmlApplication implements CommandLineRunner {

	private static final String CONTEXT_XML = "classpath:/META-INF/application-context.xml";

	@Autowired
	private HelloWorldService helloWorldService;

	/**
     * This method is the entry point of the application.
     * It prints the hello message obtained from the HelloWorldService.
     *
     * @param args the command line arguments
     */
    @Override
	public void run(String... args) {
		System.out.println(this.helloWorldService.getHelloMessage());
	}

	/**
     * The main method is the entry point of the application.
     * It initializes a SpringApplication object and sets the sources to a singleton set containing CONTEXT_XML.
     * It then runs the application with the given arguments.
     *
     * @param args the command line arguments passed to the application
     */
    public static void main(String[] args) {
		SpringApplication application = new SpringApplication();
		application.setSources(Collections.singleton(CONTEXT_XML));
		application.run(args);
	}

}
