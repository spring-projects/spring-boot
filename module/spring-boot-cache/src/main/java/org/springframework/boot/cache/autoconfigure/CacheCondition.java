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

package org.springframework.boot.cache.autoconfigure;

import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * General cache condition used with all cache configuration classes.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class CacheCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Assert.isInstanceOf(AnnotationMetadata.class, metadata);
		return extracted(context, (AnnotationMetadata) metadata);
	}

	private ConditionOutcome extracted(ConditionContext context, AnnotationMetadata metadata) {
		String sourceClass = metadata.getClassName();
		ConditionMessage.Builder message = ConditionMessage.forCondition("Cache", sourceClass);
		Environment environment = context.getEnvironment();
		try {
			BindResult<CacheType> specified = Binder.get(environment).bind("spring.cache.type", CacheType.class);
			if (!specified.isBound()) {
				return ConditionOutcome.match(message.because("automatic cache type"));
			}
			CacheType required = CacheConfigurations.getType(metadata.getClassName());
			if (specified.get() == required) {
				return ConditionOutcome.match(message.because(specified.get() + " cache type"));
			}
		}
		catch (BindException ex) {
			// Ignore
		}
		return ConditionOutcome.noMatch(message.because("unknown cache type"));
	}

}
