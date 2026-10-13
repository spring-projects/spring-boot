/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mustache.autoconfigure;

import com.samskivert.mustache.Mustache.Compiler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.mustache.servlet.view.MustacheViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(MustacheViewResolver.class)
class MustacheServletWebConfiguration {

	@Bean
	@ConditionalOnMissingBean
	MustacheViewResolver mustacheViewResolver(Compiler mustacheCompiler, MustacheProperties mustache) {
		MustacheViewResolver resolver = new MustacheViewResolver(mustacheCompiler);
		resolver.setPrefix(mustache.getPrefix());
		resolver.setSuffix(mustache.getSuffix());
		resolver.setCache(mustache.getServlet().isCache());
		if (mustache.getServlet().getContentType() != null) {
			resolver.setContentType(mustache.getServlet().getContentType().toString());
		}
		resolver.setViewNames(mustache.getViewNames());
		resolver.setExposeRequestAttributes(mustache.getServlet().isExposeRequestAttributes());
		resolver.setAllowRequestOverride(mustache.getServlet().isAllowRequestOverride());
		resolver.setAllowSessionOverride(mustache.getServlet().isAllowSessionOverride());
		resolver.setExposeSessionAttributes(mustache.getServlet().isExposeSessionAttributes());
		resolver.setExposeSpringMacroHelpers(mustache.getServlet().isExposeSpringMacroHelpers());
		resolver.setRequestContextAttribute(mustache.getRequestContextAttribute());
		resolver.setCharset(mustache.getCharset());
		resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
		return resolver;
	}

}
