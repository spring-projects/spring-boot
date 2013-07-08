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

package org.springframework.actuate.autoconfigure;

import javax.servlet.Servlet;

import org.springframework.actuate.web.BasicErrorController;
import org.springframework.actuate.web.ErrorController;
import org.springframework.autoconfigure.EnableAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.bootstrap.context.condition.ConditionalOnClass;
import org.springframework.bootstrap.context.condition.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.bootstrap.context.embedded.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to render errors via a MVC error
 * controller.
 * 
 * @author Dave Syer
 */
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
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

}
