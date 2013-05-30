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

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks that specific beans are missing.
 * 
 * @author Dave Syer
 * @see AssertMissingBean
 */
class AssertMissingBeanCondition extends OnMissingBeanCondition {

	@Override
	protected Class<?> annotationClass() {
		return AssertMissingBean.class;
	}

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		boolean result = super.matches(context, metadata);
		if (!result) {
			throw new BeanCreationException("Found existing bean for classes="
					+ getBeanClasses() + " and names=" + getBeanNames());
		}
		return result;
	}

}
