/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;

/**
 * {@link Endpoint} to expose Spring MVC mappings.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@ConfigurationProperties(prefix = "endpoints.mappings")
public class RequestMappingEndpoint extends AbstractEndpoint<Map<String, Object>>
		implements ApplicationContextAware {

	private List<AbstractUrlHandlerMapping> handlerMappings = Collections.emptyList();

	private List<AbstractHandlerMethodMapping<?>> methodMappings = Collections
			.emptyList();

	private ApplicationContext applicationContext;

	public RequestMappingEndpoint() {
		super("mappings");
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * Set the handler mappings.
	 * @param handlerMappings the handler mappings
	 */
	public void setHandlerMappings(List<AbstractUrlHandlerMapping> handlerMappings) {
		this.handlerMappings = handlerMappings;
	}

	/**
	 * Set the method mappings.
	 * @param methodMappings the method mappings
	 */
	public void setMethodMappings(List<AbstractHandlerMethodMapping<?>> methodMappings) {
		this.methodMappings = methodMappings;
	}

	@Override
	public Map<String, Object> invoke() {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		extractHandlerMappings(this.handlerMappings, result);
		extractHandlerMappings(this.applicationContext, result);
		extractMethodMappings(this.methodMappings, result);
		extractMethodMappings(this.applicationContext, result);
		return result;
	}

	@SuppressWarnings("rawtypes")
	protected void extractMethodMappings(ApplicationContext applicationContext,
			Map<String, Object> result) {
		if (applicationContext != null) {
			for (Entry<String, AbstractHandlerMethodMapping> bean : applicationContext
					.getBeansOfType(AbstractHandlerMethodMapping.class).entrySet()) {
				@SuppressWarnings("unchecked")
				Map<?, HandlerMethod> methods = bean.getValue().getHandlerMethods();
				for (Entry<?, HandlerMethod> method : methods.entrySet()) {
					Map<String, String> map = new LinkedHashMap<String, String>();
					map.put("bean", bean.getKey());
					map.put("method", method.getValue().toString());
					result.put(method.getKey().toString(), map);
				}
			}
		}
	}

	protected void extractHandlerMappings(ApplicationContext applicationContext,
			Map<String, Object> result) {
		if (applicationContext != null) {
			Map<String, AbstractUrlHandlerMapping> mappings = applicationContext
					.getBeansOfType(AbstractUrlHandlerMapping.class);
			for (Entry<String, AbstractUrlHandlerMapping> mapping : mappings.entrySet()) {
				Map<String, Object> handlers = getHandlerMap(mapping.getValue());
				for (Entry<String, Object> handler : handlers.entrySet()) {
					result.put(handler.getKey(),
							Collections.singletonMap("bean", mapping.getKey()));
				}
			}
		}
	}

	private Map<String, Object> getHandlerMap(AbstractUrlHandlerMapping mapping) {
		if (AopUtils.isCglibProxy(mapping)) {
			// If the AbstractUrlHandlerMapping is a cglib proxy we can't call
			// the final getHandlerMap() method.
			return Collections.emptyMap();
		}
		return mapping.getHandlerMap();
	}

	protected void extractHandlerMappings(
			Collection<AbstractUrlHandlerMapping> handlerMappings,
			Map<String, Object> result) {
		for (AbstractUrlHandlerMapping mapping : handlerMappings) {
			Map<String, Object> handlers = mapping.getHandlerMap();
			for (Map.Entry<String, Object> entry : handlers.entrySet()) {
				Class<? extends Object> handlerClass = entry.getValue().getClass();
				result.put(entry.getKey(),
						Collections.singletonMap("type", handlerClass.getName()));
			}
		}
	}

	protected void extractMethodMappings(
			Collection<AbstractHandlerMethodMapping<?>> methodMappings,
			Map<String, Object> result) {
		for (AbstractHandlerMethodMapping<?> mapping : methodMappings) {
			Map<?, HandlerMethod> methods = mapping.getHandlerMethods();
			for (Map.Entry<?, HandlerMethod> entry : methods.entrySet()) {
				result.put(String.valueOf(entry.getKey()), Collections
						.singletonMap("method", String.valueOf(entry.getValue())));
			}
		}
	}

}
