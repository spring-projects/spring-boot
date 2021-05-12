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

package org.springframework.boot.autoconfigure.mustache;

import com.samskivert.mustache.Mustache.Compiler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.reactive.result.view.MustacheViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.REACTIVE)
class MustacheReactiveWebConfiguration {

	@Bean
	@ConditionalOnMissingBean
	MustacheViewResolver mustacheViewResolver(Compiler mustacheCompiler, MustacheProperties mustache) {
		MustacheViewResolver resolver = new MustacheViewResolver(mustacheCompiler);
		resolver.setPrefix(mustache.getPrefix());
		resolver.setSuffix(mustache.getSuffix());
		resolver.setViewNames(mustache.getViewNames());
		resolver.setRequestContextAttribute(mustache.getRequestContextAttribute());
		resolver.setCharset(mustache.getCharsetName());
		resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
		return resolver;
	}

}
