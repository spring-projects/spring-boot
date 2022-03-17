/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.mustache;

import com.samskivert.mustache.Mustache.Compiler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.reactive.result.view.MustacheViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.REACTIVE)
class MustacheReactiveWebConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.mustache", name = "enabled", matchIfMissing = true)
	MustacheViewResolver mustacheViewResolver(Compiler mustacheCompiler, MustacheProperties mustache) {
		MustacheViewResolver resolver = new MustacheViewResolver(mustacheCompiler);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(mustache::getPrefix).to(resolver::setPrefix);
		map.from(mustache::getSuffix).to(resolver::setSuffix);
		map.from(mustache::getViewNames).to(resolver::setViewNames);
		map.from(mustache::getRequestContextAttribute).to(resolver::setRequestContextAttribute);
		map.from(mustache::getCharsetName).to(resolver::setCharset);
		map.from(mustache.getReactive()::getMediaTypes).to(resolver::setSupportedMediaTypes);
		resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
		return resolver;
	}

}
