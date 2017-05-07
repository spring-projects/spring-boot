/*
 * Copyright 2012-2015 the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class Consumer {
	
	private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);
	
	protected int countReceived_TestQueue1 = 0;
	protected int countReceived_TestQueue1_withContainerFactory = 0;
	protected int countReceived_TestTopic1 = 0;
	protected int countReceived_TestTopic1_withContainerFactory = 0;
	protected int countReceived_TestTopic1_explicit = 0;
	
	
	@JmsListener(destination = "TestQueue1")
	public void receiveQueue1(String text) {
		countReceived_TestQueue1++;
		doLog("TestQueue1", text);
	}

	/**
	 * WARNING: looks ok, but does NOT receive any message !!
	 * see receiveTopic1_withContainerFactory() instead
	 * 
	 * you "could" also change src/main/resources/application.properties 
	 * <PRE>
	 *    spring.jms.pub-sub-domain=true
	 * </PRE>
	 * ... in this case, this method would now correctly receive Topic messages, 
	 *   but receiveQueue1() would STOP receiving Queue messages!!
	 * @param text
	 */
	@JmsListener(destination = "TestTopic1")
	public void receiveTopic1(String text) {
		countReceived_TestTopic1++;
		doLog("TestTopic1", text);
	}

	/**
	 * Correction replacement for receiveTopic1()
	 * see configuration bean definition in SampleActiveMQApplication:
	 * <PRE>@Bean jmsListenerContainerTopic() { ... bean.setPubSubDomain(true); }<PRE>
	 * @param text
	 */
	@JmsListener(destination = "TestTopic1", containerFactory="jmsListenerContainerTopic")
	public void receiveTopic1_withContainerFactory(String text) {
		countReceived_TestTopic1_withContainerFactory++;
		doLog("TestTopic1 using jmsListenerContainerTopic", text);
	}

	/**
	 * Queue message listener that also works... but will consume in round-robbing 1/2 messages, other messages are consumed by receiveQueue1()
	 * (if starting another jvm process consuming events, then 1/4 messages only will be consumed in this method) 
	 */
	@JmsListener(destination = "TestQueue1", containerFactory="jmsListenerContainerQueue")
	public void receiveQueue1_withContainerFactory(String text) {
		countReceived_TestQueue1_withContainerFactory++;
		doLog("TestQueue1 using jmsListenerContainerQueue", text);
	}
	
	/**
	 * WARNING: looks ok, but does NOT receive any message, even if springframework knows that the resolved destination is a Topic !!
	 * see org.springframework.jms.config.AbstractJmsListenerContainerFactory line 181
	 * @param text
	 */
	@JmsListener(destination = "topic://TestTopic1")
	public void receiveTopic2(String text) {
		countReceived_TestTopic1_explicit++;
		doLog("topic://TestTopic1", text);
	}
	
	protected void doLog(String from, String msg) {
		LOG.info("**** RECEIVED MSG from " + from + " : " + msg);
	}

	public int getCountReceived_TestQueue1() {
		return countReceived_TestQueue1;
	}

	public int getCountReceived_TestQueue1_withContainerFactory() {
		return countReceived_TestQueue1_withContainerFactory;
	}

	public int getCountReceived_TestTopic1() {
		return countReceived_TestTopic1;
	}

	public int getCountReceived_TestTopic1_withContainerFactory() {
		return countReceived_TestTopic1_withContainerFactory;
	}

	public int getCountReceived_TestTopic1_explicit() {
		return countReceived_TestTopic1_explicit;
	}
	
	
}
