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

package org.springframework.boot.configurationprocessor.fieldvalues.javac;

import java.lang.reflect.Method;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

/**
 * Reflection based access to {@code com.sun.source.util.Trees}.
 *
 * @author Phillip Webb
 */
final class Trees extends ReflectionWrapper {

	private Trees(Object instance) {
		super("com.sun.source.util.Trees", instance);
	}

	Tree getTree(Element element) throws Exception {
		Object tree = findMethod("getTree", Element.class).invoke(getInstance(), element);
		return (tree != null) ? new Tree(tree) : null;
	}

	static Trees instance(ProcessingEnvironment env) throws Exception {
		ClassLoader classLoader = env.getClass().getClassLoader();
		Class<?> type = findClass(classLoader, "com.sun.source.util.Trees");
		Method method = findMethod(type, "instance", ProcessingEnvironment.class);
		return new Trees(method.invoke(null, env));
	}

}
