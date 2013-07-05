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

package org.springframework.zero.context.annotation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * {@link Condition} that checks for specific resources.
 * 
 * @author Dave Syer
 * @see ConditionalOnResource
 */
class OnResourceCondition implements Condition {

	private static Log logger = LogFactory.getLog(OnResourceCondition.class);

	private ResourceLoader defaultResourceLoader = new DefaultResourceLoader();

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

		String checking = ConditionLogUtils.getPrefix(logger, metadata);

		MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(
				ConditionalOnResource.class.getName(), true);
		ResourceLoader loader = context.getResourceLoader() == null ? this.defaultResourceLoader
				: context.getResourceLoader();
		if (attributes != null) {
			List<String> locations = new ArrayList<String>();
			collectValues(locations, attributes.get("resources"));
			Assert.isTrue(locations.size() > 0,
					"@ConditionalOnResource annotations must specify at least one resource location");
			for (String location : locations) {
				if (logger.isDebugEnabled()) {
					logger.debug(checking + "Checking for resource: " + location);
				}
				if (!loader.getResource(location).exists()) {
					if (logger.isDebugEnabled()) {
						logger.debug(checking + "Resource not found: " + location
								+ " (search terminated with matches=false)");
					}
					return false;
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug(checking + "Match result is: true");
		}
		return true;
	}

	private void collectValues(List<String> names, List<Object> values) {
		for (Object value : values) {
			for (Object item : (Object[]) value) {
				names.add((String) item);
			}
		}
	}

}
