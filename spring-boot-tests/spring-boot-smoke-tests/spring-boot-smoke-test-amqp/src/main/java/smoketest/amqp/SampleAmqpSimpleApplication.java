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

package smoketest.amqp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * SampleAmqpSimpleApplication class.
 */
@SpringBootApplication
@RabbitListener(queues = "foo")
public class SampleAmqpSimpleApplication {

	private static final Log logger = LogFactory.getLog(SampleAmqpSimpleApplication.class);

	/**
	 * Creates a new instance of the Sender class.
	 * @return the newly created Sender object
	 */
	@Bean
	public Sender mySender() {
		return new Sender();
	}

	/**
	 * Creates a new instance of the fooQueue.
	 * @return the fooQueue instance
	 */
	@Bean
	public Queue fooQueue() {
		return new Queue("foo");
	}

	/**
	 * Process the given payload.
	 * @param foo the payload to be processed
	 */
	@RabbitHandler
	public void process(@Payload String foo) {
		logger.info(foo);
	}

	/**
	 * This method is a bean that returns an ApplicationRunner object. The
	 * ApplicationRunner object is responsible for sending a message using the provided
	 * Sender object.
	 * @param sender The Sender object used to send the message.
	 * @return An ApplicationRunner object that sends a "Hello" message.
	 */
	@Bean
	public ApplicationRunner runner(Sender sender) {
		return (args) -> sender.send("Hello");
	}

	/**
	 * The main method is the entry point of the application. It starts the Spring
	 * application by running the SampleAmqpSimpleApplication class.
	 * @param args the command line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(SampleAmqpSimpleApplication.class, args);
	}

}
