/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet.error;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.util.HtmlUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to render errors via an MVC error
 * controller.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
// Load before the main WebMvcAutoConfiguration so that the error View is available
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
@EnableConfigurationProperties({ ServerProperties.class, ResourceProperties.class })
public class ErrorMvcAutoConfiguration {

	private final ServerProperties serverProperties;

	private final DispatcherServletPath dispatcherServletPath;

	private final List<ErrorViewResolver> errorViewResolvers;

	public ErrorMvcAutoConfiguration(ServerProperties serverProperties,
			DispatcherServletPath dispatcherServletPath,
			ObjectProvider<List<ErrorViewResolver>> errorViewResolversProvider) {
		this.serverProperties = serverProperties;
		this.dispatcherServletPath = dispatcherServletPath;
		this.errorViewResolvers = errorViewResolversProvider.getIfAvailable();
	}

	@Bean
	@ConditionalOnMissingBean(value = ErrorAttributes.class, search = SearchStrategy.CURRENT)
	public DefaultErrorAttributes errorAttributes() {
		return new DefaultErrorAttributes(
				this.serverProperties.getError().isIncludeException());
	}

	@Bean
	@ConditionalOnMissingBean(value = ErrorController.class, search = SearchStrategy.CURRENT)
	public BasicErrorController basicErrorController(ErrorAttributes errorAttributes) {
		return new BasicErrorController(errorAttributes, this.serverProperties.getError(),
				this.errorViewResolvers);
	}

	@Bean
	public ErrorPageCustomizer errorPageCustomizer() {
		return new ErrorPageCustomizer(this.serverProperties, this.dispatcherServletPath);
	}

	@Bean
	public static PreserveErrorControllerTargetClassPostProcessor preserveErrorControllerTargetClassPostProcessor() {
		return new PreserveErrorControllerTargetClassPostProcessor();
	}

	@Configuration
	static class DefaultErrorViewResolverConfiguration {

		private final ApplicationContext applicationContext;

		private final ResourceProperties resourceProperties;

		DefaultErrorViewResolverConfiguration(ApplicationContext applicationContext,
				ResourceProperties resourceProperties) {
			this.applicationContext = applicationContext;
			this.resourceProperties = resourceProperties;
		}

		@Bean
		@ConditionalOnBean(DispatcherServlet.class)
		@ConditionalOnMissingBean
		public DefaultErrorViewResolver conventionErrorViewResolver() {
			return new DefaultErrorViewResolver(this.applicationContext,
					this.resourceProperties);
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = "server.error.whitelabel", name = "enabled", matchIfMissing = true)
	@Conditional(ErrorTemplateMissingCondition.class)
	protected static class WhitelabelErrorViewConfiguration {

		private final SpelView defaultErrorView = new SpelView(
				"<html><body><h1>Whitelabel Error Page</h1>"
						+ "<p>This application has no explicit mapping for /error, so you are seeing this as a fallback.</p>"
						+ "<div id='created'>${timestamp}</div>"
						+ "<div>There was an unexpected error (type=${error}, status=${status}).</div>"
						+ "<div>${message}</div></body></html>");

		@Bean(name = "error")
		@ConditionalOnMissingBean(name = "error")
		public View defaultErrorView() {
			return this.defaultErrorView;
		}

		// If the user adds @EnableWebMvc then the bean name view resolver from
		// WebMvcAutoConfiguration disappears, so add it back in to avoid disappointment.
		@Bean
		@ConditionalOnMissingBean
		public BeanNameViewResolver beanNameViewResolver() {
			BeanNameViewResolver resolver = new BeanNameViewResolver();
			resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
			return resolver;
		}

	}

	/**
	 * {@link SpringBootCondition} that matches when no error template view is detected.
	 */
	private static class ErrorTemplateMissingCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage
					.forCondition("ErrorTemplate Missing");
			TemplateAvailabilityProviders providers = new TemplateAvailabilityProviders(
					context.getClassLoader());
			TemplateAvailabilityProvider provider = providers.getProvider("error",
					context.getEnvironment(), context.getClassLoader(),
					context.getResourceLoader());
			if (provider != null) {
				return ConditionOutcome
						.noMatch(message.foundExactly("template from " + provider));
			}
			return ConditionOutcome
					.match(message.didNotFind("error template view").atAll());
		}

	}

	/**
	 * Simple {@link View} implementation that resolves variables as SpEL expressions.
	 */
	private static class SpelView implements View {

		private static final Log logger = LogFactory.getLog(SpelView.class);

		private final NonRecursivePropertyPlaceholderHelper helper;

		private final String template;

		private volatile Map<String, Expression> expressions;

		SpelView(String template) {
			this.helper = new NonRecursivePropertyPlaceholderHelper("${", "}");
			this.template = template;
		}

		@Override
		public String getContentType() {
			return "text/html";
		}

		@Override
		public void render(Map<String, ?> model, HttpServletRequest request,
				HttpServletResponse response) throws Exception {
			if (response.isCommitted()) {
				String message = getMessage(model);
				logger.error(message);
				return;
			}
			if (response.getContentType() == null) {
				response.setContentType(getContentType());
			}
			PlaceholderResolver resolver = new ExpressionResolver(getExpressions(),
					model);
			String result = this.helper.replacePlaceholders(this.template, resolver);
			response.getWriter().append(result);
		}

		private String getMessage(Map<String, ?> model) {
			Object path = model.get("path");
			String message = "Cannot render error page for request [" + path + "]";
			if (model.get("message") != null) {
				message += " and exception [" + model.get("message") + "]";
			}
			message += " as the response has already been committed.";
			message += " As a result, the response may have the wrong status code.";
			return message;
		}

		private Map<String, Expression> getExpressions() {
			if (this.expressions == null) {
				synchronized (this) {
					ExpressionCollector expressionCollector = new ExpressionCollector();
					this.helper.replacePlaceholders(this.template, expressionCollector);
					this.expressions = expressionCollector.getExpressions();
				}
			}
			return this.expressions;
		}

	}

	/**
	 * {@link PlaceholderResolver} to collect placeholder expressions.
	 */
	private static class ExpressionCollector implements PlaceholderResolver {

		private final SpelExpressionParser parser = new SpelExpressionParser();

		private final Map<String, Expression> expressions = new HashMap<>();

		@Override
		public String resolvePlaceholder(String name) {
			this.expressions.put(name, this.parser.parseExpression(name));
			return null;
		}

		public Map<String, Expression> getExpressions() {
			return Collections.unmodifiableMap(this.expressions);
		}

	}

	/**
	 * SpEL based {@link PlaceholderResolver}.
	 */
	private static class ExpressionResolver implements PlaceholderResolver {

		private final Map<String, Expression> expressions;

		private final EvaluationContext context;

		ExpressionResolver(Map<String, Expression> expressions, Map<String, ?> map) {
			this.expressions = expressions;
			this.context = getContext(map);
		}

		private EvaluationContext getContext(Map<String, ?> map) {
			return SimpleEvaluationContext.forPropertyAccessors(new MapAccessor())
					.withRootObject(map).build();
		}

		@Override
		public String resolvePlaceholder(String placeholderName) {
			Expression expression = this.expressions.get(placeholderName);
			Object expressionValue = (expression != null)
					? expression.getValue(this.context) : null;
			return escape(expressionValue);
		}

		private String escape(Object value) {
			return HtmlUtils.htmlEscape((value != null) ? value.toString() : null);
		}

	}

	/**
	 * {@link WebServerFactoryCustomizer} that configures the server's error pages.
	 */
	private static class ErrorPageCustomizer implements ErrorPageRegistrar, Ordered {

		private final ServerProperties properties;

		private final DispatcherServletPath dispatcherServletPath;

		protected ErrorPageCustomizer(ServerProperties properties,
				DispatcherServletPath dispatcherServletPath) {
			this.properties = properties;
			this.dispatcherServletPath = dispatcherServletPath;
		}

		@Override
		public void registerErrorPages(ErrorPageRegistry errorPageRegistry) {
			ErrorPage errorPage = new ErrorPage(this.dispatcherServletPath
					.getRelativePath(this.properties.getError().getPath()));
			errorPageRegistry.addErrorPages(errorPage);
		}

		@Override
		public int getOrder() {
			return 0;
		}

	}

	/**
	 * {@link BeanFactoryPostProcessor} to ensure that the target class of ErrorController
	 * MVC beans are preserved when using AOP.
	 */
	static class PreserveErrorControllerTargetClassPostProcessor
			implements BeanFactoryPostProcessor {

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
			String[] errorControllerBeans = beanFactory
					.getBeanNamesForType(ErrorController.class, false, false);
			for (String errorControllerBean : errorControllerBeans) {
				try {
					beanFactory.getBeanDefinition(errorControllerBean).setAttribute(
							AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
				}
				catch (Throwable ex) {
					// Ignore
				}
			}
		}

	}

}
