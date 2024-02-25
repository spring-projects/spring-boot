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

package smoketest.activemq;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Consumer class.
 */
@Component
public class Consumer {

	/**
     * This method is a message listener for the "sample.queue" destination.
     * It receives a text message and prints it to the console.
     *
     * @param text the text message received from the queue
     */
    @JmsListener(destination = "sample.queue")
	public void receiveQueue(String text) {
		System.out.println(text);
	}

}
