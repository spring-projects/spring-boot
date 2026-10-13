/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mustache.autoconfigure;

import com.samskivert.mustache.Mustache.Compiler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.mustache.reactive.view.MustacheViewResolver;
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
		PropertyMapper map = PropertyMapper.get();
		map.from(mustache::getPrefix).to(resolver::setPrefix);
		map.from(mustache::getSuffix).to(resolver::setSuffix);
		map.from(mustache::getViewNames).to(resolver::setViewNames);
		map.from(mustache::getRequestContextAttribute).to(resolver::setRequestContextAttribute);
		map.from(mustache::getCharset).to(resolver::setCharset);
		map.from(mustache.getReactive()::getMediaTypes).to(resolver::setSupportedMediaTypes);
		resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
		return resolver;
	}

}
