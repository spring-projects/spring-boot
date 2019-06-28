/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;

import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link BeanNotOfRequiredTypeException}.
 *
 * @author Andy Wilkinson
 */
public class BeanNotOfRequiredTypeFailureAnalyzer extends AbstractFailureAnalyzer<BeanNotOfRequiredTypeException> {

	private static final String ACTION = "Consider injecting the bean as one of its "
			+ "interfaces or forcing the use of CGLib-based "
			+ "proxies by setting proxyTargetClass=true on @EnableAsync and/or " + "@EnableCaching.";

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, BeanNotOfRequiredTypeException cause) {
		if (!Proxy.isProxyClass(cause.getActualType())) {
			return null;
		}
		return new FailureAnalysis(getDescription(cause), ACTION, cause);
	}

	private String getDescription(BeanNotOfRequiredTypeException ex) {
		StringWriter description = new StringWriter();
		PrintWriter printer = new PrintWriter(description);
		printer.printf("The bean '%s' could not be injected as a '%s' because it is a "
				+ "JDK dynamic proxy that implements:%n", ex.getBeanName(), ex.getRequiredType().getName());
		for (Class<?> requiredTypeInterface : ex.getRequiredType().getInterfaces()) {
			printer.println("\t" + requiredTypeInterface.getName());
		}
		return description.toString();
	}

}
