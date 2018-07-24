/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import java.net.URL;
import java.security.CodeSource;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.ApplicationContextException;

/**
 * An {@link AbstractFailureAnalyzer} for incompatible servlet-api dependency.
 *
 * @author hengyunabc chen
 */
public class IncompatibleServletFailureAnalyzer
		extends AbstractInjectionFailureAnalyzer<ApplicationContextException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			ApplicationContextException cause, String description) {
		String message = cause.getMessage();

		if (message != null && message.contains("Unable to start embedded Tomcat")) {
			URL location = null;
			try {
				Class<?> clazz = Class.forName("javax.servlet.ServletContext");
				CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
				if (codeSource != null) {
					location = codeSource.getLocation();
				}
			}
			catch (ClassNotFoundException ex) {
				// ignore
			}

			if (location != null && !location.toString().contains("tomcat-embed-core")) {
				return new FailureAnalysis(
						"The servlet-api classes is incompatible, location: " + location,
						"Exclude the incompatible servlet-api dependency.", cause);
			}
		}

		return null;
	}

}
