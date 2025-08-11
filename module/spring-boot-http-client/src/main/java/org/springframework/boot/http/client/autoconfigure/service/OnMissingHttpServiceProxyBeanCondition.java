/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client.autoconfigure.service;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks for any HTTP Service proxy bean.
 *
 * @author Phillip Webb
 * @see ConditionalOnJava
 */
class OnMissingHttpServiceProxyBeanCondition extends SpringBootCondition implements ConfigurationCondition {

	static final String HTTP_SERVICE_GROUP_NAME_ATTRIBUTE = "httpServiceGroupName";

	@Override
	public ConfigurationPhase getConfigurationPhase() {
		return ConfigurationPhase.REGISTER_BEAN;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage
			.forCondition(ConditionalOnMissingHttpServiceProxyBean.class);
		BeanFactory beanFactory = context.getBeanFactory();
		while (beanFactory != null) {
			if (beanFactory instanceof ConfigurableListableBeanFactory configurableListableBeanFactory
					&& hasHttpServiceProxyBeanDefinition(configurableListableBeanFactory)) {
				return ConditionOutcome.noMatch(message.foundExactly("HTTP Service proxy bean"));
			}
			beanFactory = (beanFactory instanceof HierarchicalBeanFactory hierarchicalBeanFactory)
					? hierarchicalBeanFactory.getParentBeanFactory() : null;
		}
		return ConditionOutcome.match(message.didNotFind("").items("HTTP Service proxy beans"));
	}

	private boolean hasHttpServiceProxyBeanDefinition(ConfigurableListableBeanFactory beanFactory) {
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			if (beanDefinition.hasAttribute(HTTP_SERVICE_GROUP_NAME_ATTRIBUTE)) {
				return true;
			}
		}
		return false;
	}

}
