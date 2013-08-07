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

package org.springframework.boot.actuate.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.web.BasicErrorController;
import org.springframework.boot.actuate.web.ErrorController;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.View;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to render errors via a MVC error
 * controller.
 * 
 * @author Dave Syer
 */
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
// Ensure this loads before the main WebMvcAutoConfiguration so that the error View is
// available
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
public class ErrorMvcAutoConfiguration implements EmbeddedServletContainerCustomizer {

	@Value("${error.path:/error}")
	private String errorPath = "/error";

	@Bean
	@ConditionalOnMissingBean(ErrorController.class)
	public BasicErrorController basicErrorController() {
		return new BasicErrorController();
	}

	@Override
	public void customize(ConfigurableEmbeddedServletContainerFactory factory) {
		factory.addErrorPages(new ErrorPage(this.errorPath));
	}

	private SpelView defaultErrorView = new SpelView(
			"<html><body><h1>Whitelabel Error Page</h1>"
					+ "<p>This application has no explicit mapping for /error, so you are seeing this as a fallback.</p>"
					+ "<div id='created'>${timestamp}</div>"
					+ "<div>There was an unexpected error (type=${error}, status=${status}).</div>"
					+ "<div>${message}</div>" + "</body></html>");

	@Bean(name = "error")
	@ConditionalOnMissingBean(name = "error")
	public View defaultErrorView() {
		return this.defaultErrorView;
	}

	private static class SpelView implements View {

		private final String template;

		private final SpelExpressionParser parser = new SpelExpressionParser();

		private final StandardEvaluationContext context = new StandardEvaluationContext();

		private PropertyPlaceholderHelper helper;

		private PlaceholderResolver resolver;

		public SpelView(String template) {
			this.template = template;
			this.context.addPropertyAccessor(new MapAccessor());
			this.helper = new PropertyPlaceholderHelper("${", "}");
			this.resolver = new PlaceholderResolver() {
				public String resolvePlaceholder(String name) {
					Expression expression = SpelView.this.parser.parseExpression(name);
					Object value = expression.getValue(SpelView.this.context);
					return value == null ? null : value.toString();
				}
			};
		}

		public String getContentType() {
			return "text/html";
		}

		public void render(Map<String, ?> model, HttpServletRequest request,
				HttpServletResponse response) throws Exception {
			if (response.getContentType() == null) {
				response.setContentType(getContentType());
			}
			Map<String, Object> map = new HashMap<String, Object>(model);
			map.put("path", request.getContextPath());
			this.context.setRootObject(map);
			String result = this.helper.replacePlaceholders(this.template, this.resolver);
			response.getWriter().append(result);
		}

	}

}
