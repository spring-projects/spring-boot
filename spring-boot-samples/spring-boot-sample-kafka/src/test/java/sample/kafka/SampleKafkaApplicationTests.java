/*
 * Copyright 2012-2018 the original author or authors.
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
package sample.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.extension.OutputCapture;
import org.springframework.kafka.test.context.EmbeddedKafka;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for demo application.
 *
 * @author hcxin
 * @author Gary Russell
 * @author Stephane Nicoll
 */

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka
public class SampleKafkaApplicationTests {

	@RegisterExtension
	OutputCapture output = new OutputCapture();

	@Test
	public void testVanillaExchange() throws Exception {
		long end = System.currentTimeMillis() + 10000;
		while (!this.output.toString().contains("A simple test message")
				&& System.currentTimeMillis() < end) {
			Thread.sleep(250);
		}
		assertThat(this.output).contains("A simple test message");
	}

}
