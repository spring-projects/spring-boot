/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.zero.context.embedded;

import java.io.File;
import java.net.InetAddress;
import java.util.List;
import java.util.Set;

/**
 * Simple interface that represents customizations to an
 * {@link EmbeddedServletContainerFactory}.
 * 
 * @author Dave Syer
 * @see EmbeddedServletContainerFactory
 */
public interface ConfigurableEmbeddedServletContainerFactory extends
		EmbeddedServletContainerFactory {

	void setContextPath(String contextPath);

	String getContextPath();

	void setPort(int port);

	int getPort();

	void setAddress(InetAddress address);

	InetAddress getAddress();

	void setInitializers(List<? extends ServletContextInitializer> initializers);

	void setJspServletClassName(String jspServletClassName);

	boolean isRegisterDefaultServlet();

	void setRegisterJspServlet(boolean registerJspServlet);

	boolean isRegisterJspServlet();

	void setRegisterDefaultServlet(boolean registerDefaultServlet);

	Set<ErrorPage> getErrorPages();

	void addErrorPages(ErrorPage... errorPages);

	void setErrorPages(Set<ErrorPage> errorPages);

	File getDocumentRoot();

	void setDocumentRoot(File documentRoot);

	List<ServletContextInitializer> getInitializers();

	void addInitializers(ServletContextInitializer... initializers);

}
