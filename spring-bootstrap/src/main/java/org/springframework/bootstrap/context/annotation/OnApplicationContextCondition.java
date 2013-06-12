/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.context.annotation;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Phillip Webb
 */
public class OnApplicationContextCondition implements Condition {

	// FIXME complete or delete

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		AnnotationAttributes attributes = AnnotationAttributes
				.fromMap(metadata
						.getAnnotationAttributes(ConditionalOnApplicationContext.class
								.getName()));
		String id = (String) attributes.get("value");
		boolean createIfMissing = attributes.getBoolean("createIfMissing");
		ApplicationContext applicationContext = context.getApplicationContext();

		if (applicationContext != null) {
			if (StringUtils.hasLength(id) && applicationContext.getId().equals(id)) {
				return true;
			}
		}

		if (createIfMissing) {
			registerCreate(applicationContext, metadata);
		}

		return false;
	}

	/**
	 * @param applicationContext
	 * @param metadata
	 */
	private void registerCreate(ApplicationContext applicationContext,
			AnnotatedTypeMetadata metadata) {
		Assert.notNull(applicationContext,
				"Unable to create ApplicationContext from @ConditionalOnApplicationContext");
	}
}
