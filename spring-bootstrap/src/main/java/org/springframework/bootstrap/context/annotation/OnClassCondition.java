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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

/**
 * {@link Condition} that checks for the specific classes.
 * 
 * @author Phillip Webb
 * @see ConditionalOnClass
 */
class OnClassCondition implements Condition {

	private static Log logger = LogFactory.getLog(OnClassCondition.class);

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(
				ConditionalOnClass.class.getName(), true);
		if (attributes != null) {
			List<String> classNames = new ArrayList<String>();
			collectClassNames(classNames, attributes.get("value"));
			collectClassNames(classNames, attributes.get("name"));
			Assert.isTrue(classNames.size() > 0,
					"@ConditionalOnClass annotations must specify at least one class value");
			for (String className : classNames) {
				if (logger.isDebugEnabled()) {
					logger.debug("Checking for class: " + className);
				}
				if (!ClassUtils.isPresent(className, context.getClassLoader())) {
					if (logger.isDebugEnabled()) {
						logger.debug("Class not found: " + className
								+ " (search terminated with matches=false)");
					}
					return false;
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("All classes found (search terminated with matches=true)");
		}
		return true;
	}

	private void collectClassNames(List<String> classNames, List<Object> values) {
		for (Object value : values) {
			for (Object valueItem : (Object[]) value) {
				classNames.add(valueItem instanceof Class<?> ? ((Class<?>) valueItem)
						.getName() : valueItem.toString());
			}
		}
	}

}
