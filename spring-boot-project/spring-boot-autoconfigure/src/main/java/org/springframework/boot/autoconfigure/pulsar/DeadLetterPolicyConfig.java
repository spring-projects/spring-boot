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

package org.springframework.boot.autoconfigure.pulsar;

/**
 * Configuration properties used to specify the "dead letter queue" feature in Pulsar
 * consumers.
 *
 * @author Chris Bono
 * @since 3.2.0
 */
public class DeadLetterPolicyConfig {

	/**
	 * Maximum number of times that a message will be redelivered before being sent to the
	 * dead letter queue.
	 */
	private int maxRedeliverCount;

	/**
	 * Name of the retry topic where the failing messages will be sent.
	 */
	private String retryLetterTopic;

	/**
	 * Name of the dead topic where the failing messages will be sent.
	 */
	private String deadLetterTopic;

	/**
	 * Name of the initial subscription of the dead letter topic. When not set, the
	 * initial subscription will not be created. However, when the property is set then
	 * the broker's 'allowAutoSubscriptionCreation' must be enabled or the DLQ producer
	 * will fail.
	 */
	private String initialSubscriptionName;

	public int getMaxRedeliverCount() {
		return this.maxRedeliverCount;
	}

	public void setMaxRedeliverCount(int maxRedeliverCount) {
		this.maxRedeliverCount = maxRedeliverCount;
	}

	public String getRetryLetterTopic() {
		return this.retryLetterTopic;
	}

	public void setRetryLetterTopic(String retryLetterTopic) {
		this.retryLetterTopic = retryLetterTopic;
	}

	public String getDeadLetterTopic() {
		return this.deadLetterTopic;
	}

	public void setDeadLetterTopic(String deadLetterTopic) {
		this.deadLetterTopic = deadLetterTopic;
	}

	public String getInitialSubscriptionName() {
		return this.initialSubscriptionName;
	}

	public void setInitialSubscriptionName(String initialSubscriptionName) {
		this.initialSubscriptionName = initialSubscriptionName;
	}

}
