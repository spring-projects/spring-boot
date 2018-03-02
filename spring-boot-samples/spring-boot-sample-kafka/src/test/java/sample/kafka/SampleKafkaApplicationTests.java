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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for demo application.
 *
 * @author hcxin
 * @author Gary Russell
 * @author Stephane Nicoll
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka
public class SampleKafkaApplicationTests {

	private static final CountDownLatch latch = new CountDownLatch(1);

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void testVanillaExchange() throws Exception {
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.outputCapture.toString().contains("A simple test message"))
				.isTrue();
	}

	@TestConfiguration
	public static class Config {

		@Bean
		public Consumer consumer() {
			return new Consumer() {

				@Override
				public void processMessage(SampleMessage message) {
					super.processMessage(message);
					latch.countDown();
				}

			};
		}

	}

}
