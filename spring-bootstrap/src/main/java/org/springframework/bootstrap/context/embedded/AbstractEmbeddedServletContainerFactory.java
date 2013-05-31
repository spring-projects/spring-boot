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

package org.springframework.bootstrap.context.embedded;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@link EmbeddedServletContainerFactory} implementations.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public abstract class AbstractEmbeddedServletContainerFactory implements
		ConfigurableEmbeddedServletContainerFactory {

	private final Log logger = LogFactory.getLog(getClass());

	private String contextPath = "";

	private boolean registerDefaultServlet = true;

	private boolean registerJspServlet = true;

	private String jspServletClassName = "org.apache.jasper.servlet.JspServlet";

	private int port = 8080;

	private List<ServletContextInitializer> initializers = new ArrayList<ServletContextInitializer>();

	private File documentRoot;

	private Set<ErrorPage> errorPages = new LinkedHashSet<ErrorPage>();

	private InetAddress address;

	/**
	 * Create a new {@link AbstractEmbeddedServletContainerFactory} instance.
	 */
	public AbstractEmbeddedServletContainerFactory() {
	}

	/**
	 * Create a new {@link AbstractEmbeddedServletContainerFactory} instance with the
	 * specified port.
	 * @param port the port number for the embedded servlet container
	 */
	public AbstractEmbeddedServletContainerFactory(int port) {
		setPort(port);
	}

	/**
	 * Create a new {@link AbstractEmbeddedServletContainerFactory} instance with the
	 * specified context path and port.
	 * @param contextPath the context path for the embedded servlet container
	 * @param port the port number for the embedded servlet container
	 */
	public AbstractEmbeddedServletContainerFactory(String contextPath, int port) {
		setContextPath(contextPath);
		setPort(port);
	}

	/**
	 * Sets the context path for the embedded servlet container. The context should start
	 * with a "/" character but not end with a "/" character. The default context path can
	 * be specified using an empty string.
	 * @param contextPath the contextPath to set
	 * @see #getContextPath
	 */
	public void setContextPath(String contextPath) {
		Assert.notNull(contextPath, "ContextPath must not be null");
		if (contextPath.length() > 0) {
			if ("/".equals(contextPath)) {
				throw new IllegalArgumentException(
						"Root ContextPath must be specified using an empty string");
			}
			if (!contextPath.startsWith("/") || contextPath.endsWith("/")) {
				throw new IllegalArgumentException(
						"ContextPath must start with '/ and not end with '/'");
			}
		}
		this.contextPath = contextPath;
	}

	/**
	 * Returns the context path for the embedded servlet container. The path will start
	 * with "/" and not end with "/". The root context is represented by an empty string.
	 */
	public String getContextPath() {
		return this.contextPath;
	}

	/**
	 * Sets the port that the embedded servlet container should listen on. If not
	 * specified port '8080' will be used. Use port 0 to switch off the server completely.
	 * 
	 * @param port the port to set
	 */
	public void setPort(int port) {
		if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("Port must be between 1 and 65535");
		}
		this.port = port;
	}

	/**
	 * Returns the port that the embedded servlet container should listen on.
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * If you need the server to bind to a specific network address, provide one here.
	 * 
	 * @param address the address to set (defaults to null)
	 */
	public void setAddress(InetAddress address) {
		this.address = address;
	}

	/**
	 * @return the address the embedded container binds to
	 */
	public InetAddress getAddress() {
		return this.address;
	}

	/**
	 * Sets {@link ServletContextInitializer} that should be applied in addition to
	 * {@link #getEmbdeddedServletContainer(ServletContextInitializer...)} parameters.
	 * This method will replace any previously set or added initializers.
	 * @param initializers the initializers to set
	 * @see #addInitializers
	 * @see #getInitializers
	 */
	public void setInitializers(List<? extends ServletContextInitializer> initializers) {
		Assert.notNull(initializers, "Initializers must not be null");
		this.initializers = new ArrayList<ServletContextInitializer>(initializers);
	}

	/**
	 * Add {@link ServletContextInitializer}s to those that should be applied in addition
	 * to {@link #getEmbdeddedServletContainer(ServletContextInitializer...)} parameters.
	 * @param initializers the initializers to add
	 * @see #setInitializers
	 * @see #getInitializers
	 */
	@Override
	public void addInitializers(ServletContextInitializer... initializers) {
		Assert.notNull(initializers, "Initializers must not be null");
		this.initializers.addAll(Arrays.asList(initializers));
	}

	/**
	 * Returns a mutable list of {@link ServletContextInitializer} that should be applied
	 * in addition to {@link #getEmbdeddedServletContainer(ServletContextInitializer...)}
	 * parameters.
	 * @return the initializers
	 */
	@Override
	public List<ServletContextInitializer> getInitializers() {
		return this.initializers;
	}

	/**
	 * Sets the document root folder which will be used by the web context to serve static
	 * files.
	 * @param documentRoot the document root or {@code null} if not required
	 */
	@Override
	public void setDocumentRoot(File documentRoot) {
		this.documentRoot = documentRoot;
	}

	/**
	 * Returns the document root which will be used by the web context to serve static
	 * files.
	 */
	@Override
	public File getDocumentRoot() {
		return this.documentRoot;
	}

	/**
	 * Sets the error pages that will be used when handling exceptions.
	 * @param errorPages the error pages
	 */
	@Override
	public void setErrorPages(Set<ErrorPage> errorPages) {
		Assert.notNull(errorPages, "ErrorPages must not be null");
		this.errorPages = new LinkedHashSet<ErrorPage>(errorPages);
	}

	/**
	 * Adds error pages that will be used when handling exceptions.
	 * @param errorPages the error pages
	 */
	@Override
	public void addErrorPages(ErrorPage... errorPages) {
		Assert.notNull(this.initializers, "ErrorPages must not be null");
		this.errorPages.addAll(Arrays.asList(errorPages));
	}

	/**
	 * Returns a mutable set of {@link ErrorPage}s that will be used when handling
	 * exceptions.
	 */
	@Override
	public Set<ErrorPage> getErrorPages() {
		return this.errorPages;
	}

	/**
	 * Set if the DefaultServlet should be registered. Defaults to {@code true} so that
	 * files from the {@link #setDocumentRoot(File) document root} will be served.
	 * @param registerDefaultServlet if the default servlet should be registered
	 */
	@Override
	public void setRegisterDefaultServlet(boolean registerDefaultServlet) {
		this.registerDefaultServlet = registerDefaultServlet;
	}

	/**
	 * Flag to indicate that the JSP servlet should be registered if available on the
	 * classpath.
	 * 
	 * @return true if the JSP servlet is to be registered
	 */
	@Override
	public boolean isRegisterJspServlet() {
		return this.registerJspServlet;
	}

	/**
	 * Set if the JspServlet should be registered if it is on the classpath. Defaults to
	 * {@code true} so that files from the {@link #setDocumentRoot(File) document root}
	 * will be served.
	 * @param registerJspServlet if the JSP servlet should be registered
	 */
	@Override
	public void setRegisterJspServlet(boolean registerJspServlet) {
		this.registerJspServlet = registerJspServlet;
	}

	/**
	 * Flag to indicate that the default servlet should be registered.
	 * 
	 * @return true if the default servlet is to be registered
	 */
	@Override
	public boolean isRegisterDefaultServlet() {
		return this.registerDefaultServlet;
	}

	/**
	 * The class name for the jsp servlet if used. If
	 * {@link #setRegisterJspServlet(boolean) <code>registerJspServlet</code>} is true
	 * <b>and</b> this class is on the classpath then it will be registered. Since both
	 * Tomcat and Jetty use Jasper for their JSP implementation the default is
	 * <code>org.apache.jasper.servlet.JspServlet</code>.
	 * 
	 * @param jspServletClassName the class name for the JSP servlet if used
	 */
	@Override
	public void setJspServletClassName(String jspServletClassName) {
		this.jspServletClassName = jspServletClassName;
	}

	/**
	 * @return the JS{ servlet class name
	 */
	protected String getJspServletClassName() {
		return this.jspServletClassName;
	}

	/**
	 * Utility method that can be used by subclasses wishing to combine the specified
	 * {@link ServletContextInitializer} parameters with those defined in this instance.
	 * @param initializers the initializers to merge
	 * @return a complete set of merged initializers (with the specified parameters
	 * appearing first)
	 */
	protected final ServletContextInitializer[] mergeInitializers(
			ServletContextInitializer... initializers) {
		List<ServletContextInitializer> mergedInitializers = new ArrayList<ServletContextInitializer>();
		mergedInitializers.addAll(Arrays.asList(initializers));
		mergedInitializers.addAll(getInitializers());
		return mergedInitializers
				.toArray(new ServletContextInitializer[mergedInitializers.size()]);
	}

	/**
	 * Returns the absolute document root when it points to a valid folder, logging a
	 * warning and returning {@code null} otherwise.
	 */
	protected final File getValidDocumentRoot() {
		File[] roots = new File[] { getDocumentRoot(), new File("src/main/webapp"),
				new File("public"), new File("static") };
		for (File root : roots) {
			if (root != null && root.exists() && root.isDirectory()) {
				return root.getAbsoluteFile();
			}
		}
		if (this.logger.isWarnEnabled()) {
			this.logger.warn("None of the document roots " + roots
					+ " point to a directory and will be ignored.");
		}
		return null;
	}

}
