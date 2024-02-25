/*
 * Copyright 2012-2023 the original author or authors.
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

package smoketest.groovytemplates;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;

/**
 * SampleGroovyTemplateApplication class.
 */
@SpringBootApplication
public class SampleGroovyTemplateApplication {

	/**
	 * Creates a new instance of InMemoryMessageRepository and returns it as a
	 * MessageRepository.
	 * @return the newly created InMemoryMessageRepository instance
	 */
	@Bean
	public MessageRepository messageRepository() {
		return new InMemoryMessageRepository();
	}

	/**
	 * Returns a message converter that converts a string ID to a Message object.
	 * @return the message converter
	 */
	@Bean
	public Converter<String, Message> messageConverter() {
		return new Converter<>() {
			@Override
			public Message convert(String id) {
				return messageRepository().findMessage(Long.valueOf(id));
			}
		};
	}

	/**
	 * The main method is the entry point of the application. It starts the Spring Boot
	 * application by running the SpringApplication.run() method.
	 * @param args the command line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(SampleGroovyTemplateApplication.class, args);
	}

}
