/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.launchscript;

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

/**
 * LaunchScriptTestApplication class.
 */
public class LaunchScriptTestApplication {

	/**
     * The main method of the LaunchScriptTestApplication class.
     * 
     * @param args an array of command-line arguments
     * @throws LifecycleException if an error occurs during the lifecycle of the application
     */
    public static void main(String[] args) throws LifecycleException {
		System.out.println("Starting " + LaunchScriptTestApplication.class.getSimpleName() + " (" + findSource() + ")");
		Tomcat tomcat = new Tomcat();
		tomcat.getConnector().setPort(getPort(args));
		Context context = tomcat.addContext(getContextPath(args), null);
		tomcat.addServlet(context.getPath(), "test", new HttpServlet() {

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {
				resp.getWriter().println("Launched");
			}

		});
		context.addServletMappingDecoded("/", "test");
		tomcat.start();
	}

	/**
     * Finds the source URL of the LaunchScriptTestApplication class.
     * 
     * @return the source URL of the LaunchScriptTestApplication class, or null if not found
     */
    private static URL findSource() {
		try {
			ProtectionDomain domain = LaunchScriptTestApplication.class.getProtectionDomain();
			CodeSource codeSource = (domain != null) ? domain.getCodeSource() : null;
			return (codeSource != null) ? codeSource.getLocation() : null;
		}
		catch (Exception ex) {
		}
		return null;
	}

	/**
     * Returns the port number to be used for the server.
     * 
     * @param args the command line arguments
     * @return the port number
     */
    private static int getPort(String[] args) {
		String port = getProperty(args, "server.port");
		return (port != null) ? Integer.parseInt(port) : 8080;
	}

	/**
     * Returns the context path of the server servlet.
     * 
     * @param args the command line arguments
     * @return the context path if it is set, otherwise an empty string
     */
    private static String getContextPath(String[] args) {
		String contextPath = getProperty(args, "server.servlet.context-path");
		return (contextPath != null) ? contextPath : "";
	}

	/**
     * Retrieves the value of a specified property from the system properties or command line arguments.
     * 
     * @param args     the command line arguments
     * @param property the name of the property to retrieve
     * @return the value of the property, or null if not found
     */
    private static String getProperty(String[] args, String property) {
		String value = System.getProperty(property);
		if (value != null) {
			return value;
		}
		String prefix = "--" + property + "=";
		for (String arg : args) {
			if (arg.startsWith(prefix)) {
				return arg.substring(prefix.length());
			}
		}
		return null;
	}

}
