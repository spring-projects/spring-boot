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

package sample.activemq;

import javax.jms.JMSException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests for demo application.
 *
 * @author Eddú Meléndez
 */
@RunWith(SpringRunner.class)
// @SpringBootTest
@SpringApplicationConfiguration(SampleActiveMQApplication.class)
public class SampleActiveMqTests {

	@Autowired
	private Producer producer;

	@Autowired
	private Consumer consumer;

	@Value("${sample.producer.sendRepeatCount:2}")
	protected int sendRepeatCount = 2;
	
	@Test
	public void sendSimpleMessage() throws InterruptedException, JMSException {
		// this.producer.send("Test message"); // see messages already sent in main... or use @SpringBootTest ??
		Thread.sleep(1000L);
		
		Assert.assertTrue(0 < consumer.getCountReceived_TestQueue1());
		Assert.assertEquals(0, consumer.getCountReceived_TestTopic1()); // this receiver does not work!
		Assert.assertTrue( 0 < consumer.getCountReceived_TestTopic1_withContainerFactory());
		
		Assert.assertEquals(1 + sendRepeatCount, (consumer.getCountReceived_TestQueue1() + consumer.getCountReceived_TestQueue1_withContainerFactory()));
		Assert.assertEquals(1 + sendRepeatCount, consumer.getCountReceived_TestTopic1_withContainerFactory());
		
	}

}
