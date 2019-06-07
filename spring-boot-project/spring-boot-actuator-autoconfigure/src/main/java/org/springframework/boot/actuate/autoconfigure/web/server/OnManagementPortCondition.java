/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.server;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.web.reactive.context.ConfigurableReactiveWebApplicationContext;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link SpringBootCondition} that matches when the management server is running on a
 * different port.
 *
 * @author Andy Wilkinson
 */
class OnManagementPortCondition extends SpringBootCondition {

	private static final String CLASS_NAME_WEB_APPLICATION_CONTEXT = "org.springframework.web.context.WebApplicationContext";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage.forCondition("Management Port");
		if (!isWebApplicationContext(context)) {
			return ConditionOutcome.noMatch(message.because("non web application context"));
		}
		Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnManagementPort.class.getName());
		ManagementPortType requiredType = (ManagementPortType) attributes.get("value");
		ManagementPortType actualType = ManagementPortType.get(context.getEnvironment());
		if (actualType == requiredType) {
			return ConditionOutcome
					.match(message.because("actual port type (" + actualType + ") matched required type"));
		}
		return ConditionOutcome.noMatch(message
				.because("actual port type (" + actualType + ") did not match required type (" + requiredType + ")"));
	}

	private boolean isWebApplicationContext(ConditionContext context) {
		ResourceLoader resourceLoader = context.getResourceLoader();
		if (resourceLoader instanceof ConfigurableReactiveWebApplicationContext) {
			return true;
		}
		if (!ClassUtils.isPresent(CLASS_NAME_WEB_APPLICATION_CONTEXT, context.getClassLoader())) {
			return false;
		}
		return WebApplicationContext.class.isInstance(resourceLoader);
	}

}
