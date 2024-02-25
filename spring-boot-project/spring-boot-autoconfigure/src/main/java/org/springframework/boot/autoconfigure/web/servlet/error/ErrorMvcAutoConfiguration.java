/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet.error;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
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
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.util.HtmlUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to render errors through an MVC
 * error controller.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author Scott Frederick
 * @since 1.0.0
 */
// Load before the main WebMvcAutoConfiguration so that the error View is available
@AutoConfiguration(before = WebMvcAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
@EnableConfigurationProperties({ ServerProperties.class, WebMvcProperties.class })
public class ErrorMvcAutoConfiguration {

	private final ServerProperties serverProperties;

	/**
	 * Constructs a new ErrorMvcAutoConfiguration object with the specified
	 * ServerProperties.
	 * @param serverProperties the ServerProperties object to be used for configuration
	 */
	public ErrorMvcAutoConfiguration(ServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	/**
	 * Creates a new instance of DefaultErrorAttributes if no other bean of type
	 * ErrorAttributes is present in the application context.
	 * @return the DefaultErrorAttributes instance
	 */
	@Bean
	@ConditionalOnMissingBean(value = ErrorAttributes.class, search = SearchStrategy.CURRENT)
	public DefaultErrorAttributes errorAttributes() {
		return new DefaultErrorAttributes();
	}

	/**
	 * Create a bean for BasicErrorController if no bean of type ErrorController is
	 * already present in the application context. This controller handles basic error
	 * handling and provides error attributes and error view resolvers.
	 * @param errorAttributes the error attributes to be used by the BasicErrorController
	 * @param errorViewResolvers the error view resolvers to be used by the
	 * BasicErrorController
	 * @return the BasicErrorController bean
	 */
	@Bean
	@ConditionalOnMissingBean(value = ErrorController.class, search = SearchStrategy.CURRENT)
	public BasicErrorController basicErrorController(ErrorAttributes errorAttributes,
			ObjectProvider<ErrorViewResolver> errorViewResolvers) {
		return new BasicErrorController(errorAttributes, this.serverProperties.getError(),
				errorViewResolvers.orderedStream().toList());
	}

	/**
	 * Creates an instance of ErrorPageCustomizer with the specified serverProperties and
	 * dispatcherServletPath.
	 * @param serverProperties the server properties
	 * @param dispatcherServletPath the dispatcher servlet path
	 * @return the ErrorPageCustomizer instance
	 */
	@Bean
	public ErrorPageCustomizer errorPageCustomizer(DispatcherServletPath dispatcherServletPath) {
		return new ErrorPageCustomizer(this.serverProperties, dispatcherServletPath);
	}

	/**
	 * Creates a new instance of {@link PreserveErrorControllerTargetClassPostProcessor}.
	 * This method is used to configure the
	 * {@link PreserveErrorControllerTargetClassPostProcessor} bean.
	 * @return The configured {@link PreserveErrorControllerTargetClassPostProcessor}
	 * bean.
	 */
	@Bean
	public static PreserveErrorControllerTargetClassPostProcessor preserveErrorControllerTargetClassPostProcessor() {
		return new PreserveErrorControllerTargetClassPostProcessor();
	}

	/**
	 * DefaultErrorViewResolverConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties({ WebProperties.class, WebMvcProperties.class })
	static class DefaultErrorViewResolverConfiguration {

		private final ApplicationContext applicationContext;

		private final Resources resources;

		/**
		 * Constructs a new DefaultErrorViewResolverConfiguration with the specified
		 * ApplicationContext and WebProperties.
		 * @param applicationContext the ApplicationContext used for resolving resources
		 * @param webProperties the WebProperties used for configuring resources
		 */
		DefaultErrorViewResolverConfiguration(ApplicationContext applicationContext, WebProperties webProperties) {
			this.applicationContext = applicationContext;
			this.resources = webProperties.getResources();
		}

		/**
		 * Creates a new instance of DefaultErrorViewResolver if a DispatcherServlet bean
		 * is present and an ErrorViewResolver bean is missing. This resolver is
		 * responsible for resolving error views based on convention.
		 * @return the created DefaultErrorViewResolver instance
		 */
		@Bean
		@ConditionalOnBean(DispatcherServlet.class)
		@ConditionalOnMissingBean(ErrorViewResolver.class)
		DefaultErrorViewResolver conventionErrorViewResolver() {
			return new DefaultErrorViewResolver(this.applicationContext, this.resources);
		}

	}

	/**
	 * WhitelabelErrorViewConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "server.error.whitelabel", name = "enabled", matchIfMissing = true)
	@Conditional(ErrorTemplateMissingCondition.class)
	protected static class WhitelabelErrorViewConfiguration {

		private final StaticView defaultErrorView = new StaticView();

		/**
		 * Returns the default error view.
		 *
		 * This method is annotated with @Bean and @ConditionalOnMissingBean to ensure
		 * that it is only created if no other bean with the name "error" is present.
		 * @return the default error view
		 */
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
	private static final class ErrorTemplateMissingCondition extends SpringBootCondition {

		/**
		 * Determines the match outcome for the ErrorTemplateMissingCondition.
		 * @param context the condition context
		 * @param metadata the annotated type metadata
		 * @return the condition outcome
		 */
		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("ErrorTemplate Missing");
			TemplateAvailabilityProviders providers = new TemplateAvailabilityProviders(context.getClassLoader());
			TemplateAvailabilityProvider provider = providers.getProvider("error", context.getEnvironment(),
					context.getClassLoader(), context.getResourceLoader());
			if (provider != null) {
				return ConditionOutcome.noMatch(message.foundExactly("template from " + provider));
			}
			return ConditionOutcome.match(message.didNotFind("error template view").atAll());
		}

	}

	/**
	 * Simple {@link View} implementation that writes a default HTML error page.
	 */
	private static final class StaticView implements View {

		private static final MediaType TEXT_HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);

		private static final Log logger = LogFactory.getLog(StaticView.class);

		/**
		 * Renders the error page with the given model, request, and response.
		 * @param model the model containing the error information
		 * @param request the HttpServletRequest object
		 * @param response the HttpServletResponse object
		 * @throws Exception if an error occurs during rendering
		 */
		@Override
		public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
				throws Exception {
			if (response.isCommitted()) {
				String message = getMessage(model);
				logger.error(message);
				return;
			}
			response.setContentType(TEXT_HTML_UTF8.toString());
			StringBuilder builder = new StringBuilder();
			Object timestamp = model.get("timestamp");
			Object message = model.get("message");
			Object trace = model.get("trace");
			if (response.getContentType() == null) {
				response.setContentType(getContentType());
			}
			builder.append("<html><body><h1>Whitelabel Error Page</h1>")
				.append("<p>This application has no explicit mapping for /error, so you are seeing this as a fallback.</p>")
				.append("<div id='created'>")
				.append(timestamp)
				.append("</div>")
				.append("<div>There was an unexpected error (type=")
				.append(htmlEscape(model.get("error")))
				.append(", status=")
				.append(htmlEscape(model.get("status")))
				.append(").</div>");
			if (message != null) {
				builder.append("<div>").append(htmlEscape(message)).append("</div>");
			}
			if (trace != null) {
				builder.append("<div style='white-space:pre-wrap;'>").append(htmlEscape(trace)).append("</div>");
			}
			builder.append("</body></html>");
			response.getWriter().append(builder.toString());
		}

		/**
		 * Escapes special characters in the given input string to prevent HTML injection
		 * attacks.
		 * @param input the input object to be escaped
		 * @return the escaped string, or null if the input is null
		 */
		private String htmlEscape(Object input) {
			return (input != null) ? HtmlUtils.htmlEscape(input.toString()) : null;
		}

		/**
		 * Returns a message indicating that the error page cannot be rendered for the
		 * given request and exception.
		 * @param model the model containing the request path and exception message
		 * @return the error message
		 */
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

		/**
		 * Returns the content type of the static view.
		 * @return the content type as a string, which is "text/html"
		 */
		@Override
		public String getContentType() {
			return "text/html";
		}

	}

	/**
	 * {@link WebServerFactoryCustomizer} that configures the server's error pages.
	 */
	static class ErrorPageCustomizer implements ErrorPageRegistrar, Ordered {

		private final ServerProperties properties;

		private final DispatcherServletPath dispatcherServletPath;

		/**
		 * Constructs a new ErrorPageCustomizer with the specified ServerProperties and
		 * DispatcherServletPath.
		 * @param properties the ServerProperties to be used by the ErrorPageCustomizer
		 * @param dispatcherServletPath the DispatcherServletPath to be used by the
		 * ErrorPageCustomizer
		 */
		protected ErrorPageCustomizer(ServerProperties properties, DispatcherServletPath dispatcherServletPath) {
			this.properties = properties;
			this.dispatcherServletPath = dispatcherServletPath;
		}

		/**
		 * Registers error pages for the application.
		 * @param errorPageRegistry the error page registry to register the error pages
		 * with
		 */
		@Override
		public void registerErrorPages(ErrorPageRegistry errorPageRegistry) {
			ErrorPage errorPage = new ErrorPage(
					this.dispatcherServletPath.getRelativePath(this.properties.getError().getPath()));
			errorPageRegistry.addErrorPages(errorPage);
		}

		/**
		 * Returns the order value for this ErrorPageCustomizer.
		 *
		 * The order value determines the order in which multiple ErrorPageCustomizer
		 * beans are applied.
		 * @return the order value for this ErrorPageCustomizer
		 */
		@Override
		public int getOrder() {
			return 0;
		}

	}

	/**
	 * {@link BeanFactoryPostProcessor} to ensure that the target class of ErrorController
	 * MVC beans are preserved when using AOP.
	 */
	static class PreserveErrorControllerTargetClassPostProcessor implements BeanFactoryPostProcessor {

		/**
		 * Post-processes the bean factory to preserve the target class attribute for
		 * ErrorController beans. This ensures that the target class is preserved when
		 * creating proxies for ErrorController beans.
		 * @param beanFactory the bean factory to post-process
		 * @throws BeansException if an error occurs during post-processing
		 */
		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			String[] errorControllerBeans = beanFactory.getBeanNamesForType(ErrorController.class, false, false);
			for (String errorControllerBean : errorControllerBeans) {
				try {
					beanFactory.getBeanDefinition(errorControllerBean)
						.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
				}
				catch (Throwable ex) {
					// Ignore
				}
			}
		}

	}

}
