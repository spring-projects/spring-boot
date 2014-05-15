/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.transport.vm.VMTransportFactory;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to integrate with
 * an ActiveMQ broker.
 *
 * <p>Validates that the classpath contain the necessary classes before
 * starting an embedded broker.
 *
 * @author Stephane Nicoll
 */
@Configuration
@AutoConfigureBefore(JmsTemplateAutoConfiguration.class)
@ConditionalOnClass({ConnectionFactory.class, ActiveMQConnectionFactory.class})
@ConditionalOnMissingBean(ConnectionFactory.class)
public class ActiveMQAutoConfiguration {

	@Configuration
	@ConditionalOnClass(VMTransportFactory.class)
	@Conditional(EmbeddedBrokerCondition.class)
	@Import(ActiveMQConnectionFactoryConfiguration.class)
	protected static class EmbeddedBroker {
	}

	@Configuration
	@Conditional(NonEmbeddedBrokerCondition.class)
	@Import(ActiveMQConnectionFactoryConfiguration.class)
	protected static class NetworkBroker {
	}

	static abstract class BrokerTypeCondition extends SpringBootCondition {
		private final boolean embedded;

		BrokerTypeCondition(boolean embedded) {
			this.embedded = embedded;
		}

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			String brokerUrl = ActiveMQProperties.determineBrokerUrl(context.getEnvironment());
			boolean match = brokerUrl.contains("vm://");
			boolean outcome = (match == this.embedded);
			return new ConditionOutcome(outcome, buildMessage(brokerUrl, outcome));
		}

		protected String buildMessage(String brokerUrl, boolean outcome) {
			String brokerType = embedded ? "Embedded" : "Network";
			String detected = outcome ? "detected" : "not detected";
			return brokerType + " ActiveMQ broker " + detected + " - brokerUrl '" + brokerUrl + "'";
		}
	}

	static class EmbeddedBrokerCondition extends BrokerTypeCondition {

		EmbeddedBrokerCondition() {
			super(true);
		}
	}

	static class NonEmbeddedBrokerCondition extends BrokerTypeCondition {

		NonEmbeddedBrokerCondition() {
			super(false);
		}
	}

}
