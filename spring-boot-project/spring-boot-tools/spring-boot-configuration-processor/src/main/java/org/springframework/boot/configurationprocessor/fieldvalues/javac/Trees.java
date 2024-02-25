/*
 * Copyright 2012-2020 the original author or authors.
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

/**
 * Reflection based access to {@code com.sun.source.util.Trees}.
 *
 * @author Phillip Webb
 */
final class Trees extends ReflectionWrapper {

	/**
     * Constructs a new Trees object with the specified instance.
     * 
     * @param instance the instance to be associated with the Trees object
     */
    private Trees(Object instance) {
		super("com.sun.source.util.Trees", instance);
	}

	/**
     * Retrieves the tree associated with the given element.
     * 
     * @param element the element for which to retrieve the tree
     * @return the tree associated with the element, or null if no tree is found
     * @throws Exception if an error occurs during the retrieval process
     */
    Tree getTree(Element element) throws Exception {
		Object tree = findMethod("getTree", Element.class).invoke(getInstance(), element);
		return (tree != null) ? new Tree(tree) : null;
	}

	/**
     * Returns an instance of the Trees class.
     * 
     * @param env the ProcessingEnvironment object used to obtain the instance
     * @return an instance of the Trees class
     * @throws Exception if an error occurs while obtaining the instance
     */
    static Trees instance(ProcessingEnvironment env) throws Exception {
		try {
			ClassLoader classLoader = env.getClass().getClassLoader();
			Class<?> type = findClass(classLoader, "com.sun.source.util.Trees");
			Method method = findMethod(type, "instance", ProcessingEnvironment.class);
			return new Trees(method.invoke(null, env));
		}
		catch (Exception ex) {
			return instance(unwrap(env));
		}
	}

	/**
     * Unwraps the delegate ProcessingEnvironment from the given wrapper ProcessingEnvironment.
     * 
     * @param wrapper the wrapper ProcessingEnvironment to unwrap
     * @return the delegate ProcessingEnvironment
     * @throws Exception if an error occurs during the unwrapping process
     */
    private static ProcessingEnvironment unwrap(ProcessingEnvironment wrapper) throws Exception {
		Field delegateField = wrapper.getClass().getDeclaredField("delegate");
		delegateField.setAccessible(true);
		return (ProcessingEnvironment) delegateField.get(wrapper);
	}

}
