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

public class LaunchScriptTestApplication {

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

	private static int getPort(String[] args) {
		String port = getProperty(args, "server.port");
		return (port != null) ? Integer.parseInt(port) : 8080;
	}

	private static String getContextPath(String[] args) {
		String contextPath = getProperty(args, "server.servlet.context-path");
		return (contextPath != null) ? contextPath : "";
	}

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
