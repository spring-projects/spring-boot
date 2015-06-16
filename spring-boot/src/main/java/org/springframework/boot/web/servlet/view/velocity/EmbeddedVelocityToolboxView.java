/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.web.servlet.view.velocity;

import java.io.InputStream;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.view.velocity.VelocityToolboxView;

/**
 * Extended version of {@link VelocityToolboxView} that can load toolbox locations from
 * the classpath as well as the servlet context. This is useful when run an embedded web
 * server.
 *
 * @author Phillip Webb
 * @since 1.2.5
 */
@SuppressWarnings("deprecation")
public class EmbeddedVelocityToolboxView extends VelocityToolboxView {

	@Override
	protected Context createVelocityContext(Map<String, Object> model,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		org.apache.velocity.tools.view.context.ChainedContext context = new org.apache.velocity.tools.view.context.ChainedContext(
				new VelocityContext(model), getVelocityEngine(), request, response,
				getServletContext());
		if (getToolboxConfigLocation() != null) {
			setContextToolbox(context);
		}
		return context;
	}

	@SuppressWarnings("unchecked")
	private void setContextToolbox(
			org.apache.velocity.tools.view.context.ChainedContext context) {
		org.apache.velocity.tools.view.ToolboxManager toolboxManager = org.apache.velocity.tools.view.servlet.ServletToolboxManager
				.getInstance(getToolboxConfigFileAwareServletContext(),
						getToolboxConfigLocation());
		Map<String, Object> toolboxContext = toolboxManager.getToolbox(context);
		context.setToolbox(toolboxContext);
	}

	private ServletContext getToolboxConfigFileAwareServletContext() {
		ProxyFactory factory = new ProxyFactory();
		factory.setTarget(getServletContext());
		factory.addAdvice(new GetResourceMethodInterceptor(getToolboxConfigLocation()));
		return (ServletContext) factory.getProxy();
	}

	/**
	 * {@link MethodInterceptor} to allow the calls to getResourceAsStream() to resolve
	 * the toolboxFile from the classpath.
	 */
	private static class GetResourceMethodInterceptor implements MethodInterceptor {

		private final String toolboxFile;

		public GetResourceMethodInterceptor(String toolboxFile) {
			if (toolboxFile != null && !toolboxFile.startsWith("/")) {
				toolboxFile = "/" + toolboxFile;
			}
			this.toolboxFile = toolboxFile;
		}

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			if (invocation.getMethod().getName().equals("getResourceAsStream")
					&& invocation.getArguments()[0].equals(this.toolboxFile)) {
				InputStream inputStream = (InputStream) invocation.proceed();
				if (inputStream == null) {
					try {
						inputStream = new ClassPathResource(this.toolboxFile, Thread
								.currentThread().getContextClassLoader())
								.getInputStream();
					}
					catch (Exception ex) {
						// Ignore
					}
				}
				return inputStream;
			}
			return invocation.proceed();
		}

	}

}
