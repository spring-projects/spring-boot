/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.web.embedded.tomcat;

import java.lang.reflect.Method;

import org.apache.catalina.Context;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Tomcat specific management for an {@link ErrorPage}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
class TomcatErrorPage {

	private static final String ERROR_PAGE_CLASS = "org.apache.tomcat.util.descriptor.web.ErrorPage";

	private final String location;

	private final String exceptionType;

	private final int errorCode;

	private final Object nativePage;

	TomcatErrorPage(ErrorPage errorPage) {
		this.location = errorPage.getPath();
		this.exceptionType = errorPage.getExceptionName();
		this.errorCode = errorPage.getStatusCode();
		this.nativePage = createNativePage();
	}

	private Object createNativePage() {
		try {
			if (ClassUtils.isPresent(ERROR_PAGE_CLASS, null)) {
				return BeanUtils
						.instantiateClass(ClassUtils.forName(ERROR_PAGE_CLASS, null));
			}
		}
		catch (ClassNotFoundException | LinkageError ex) {
			// Swallow and continue
		}
		return null;
	}

	public void addToContext(Context context) {
		Assert.state(this.nativePage != null,
				"No Tomcat 8 detected so no native error page exists");
		if (ClassUtils.isPresent(ERROR_PAGE_CLASS, null)) {
			org.apache.tomcat.util.descriptor.web.ErrorPage errorPage = (org.apache.tomcat.util.descriptor.web.ErrorPage) this.nativePage;
			errorPage.setLocation(this.location);
			errorPage.setErrorCode(this.errorCode);
			errorPage.setExceptionType(this.exceptionType);
			context.addErrorPage(errorPage);
		}
		else {
			callMethod(this.nativePage, "setLocation", this.location, String.class);
			callMethod(this.nativePage, "setErrorCode", this.errorCode, int.class);
			callMethod(this.nativePage, "setExceptionType", this.exceptionType,
					String.class);
			callMethod(context, "addErrorPage", this.nativePage,
					this.nativePage.getClass());
		}
	}

	private void callMethod(Object target, String name, Object value, Class<?> type) {
		Method method = ReflectionUtils.findMethod(target.getClass(), name, type);
		ReflectionUtils.invokeMethod(method, target, value);
	}

}
