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

package org.springframework.boot.developertools.remote.client;

import javax.net.ServerSocketFactory;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.developertools.autoconfigure.RemoteDeveloperToolsProperties;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition used to check that the actual local port is available.
 */
class LocalDebugPortAvailableCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				context.getEnvironment(), "spring.developertools.remote.debug.");
		Integer port = resolver.getProperty("local-port", Integer.class);
		if (port == null) {
			port = RemoteDeveloperToolsProperties.Debug.DEFAULT_LOCAL_PORT;
		}
		if (isPortAvailable(port)) {
			return ConditionOutcome.match("Local debug port availble");
		}
		return ConditionOutcome.noMatch("Local debug port unavailble");
	}

	private boolean isPortAvailable(int port) {
		try {
			ServerSocketFactory.getDefault().createServerSocket(port).close();
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

}
