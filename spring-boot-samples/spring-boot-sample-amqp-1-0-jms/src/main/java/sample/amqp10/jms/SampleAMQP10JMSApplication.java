/*
 * Copyright 2012-2016 the original author or authors.
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

package sample.amqp10.jms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

/**
 * Simple Hello World example that sends and receives a message using AMQP 1.0 mapped to
 * JMS.
 *
 * @author Timothy Bish
 */
@SpringBootApplication
@EnableJms
public class SampleAMQP10JMSApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleAMQP10JMSApplication.class, args);
	}
}
