/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.packaging.nativeimage.advanced.customhints;

import java.lang.reflect.Method;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.util.ReflectionUtils;

public class MyRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		// Register method for reflection
		Method method = ReflectionUtils.findMethod(MyClass.class, "sayHello", String.class);
		hints.reflection().registerMethod(method, ExecutableMode.INVOKE);

		// Register type for java serialization
		hints.reflection().registerJavaSerialization(MySerializableClass.class);

		// Register resources
		hints.resources().registerPattern("my-resource.txt");

		// Register proxy
		hints.proxies().registerJdkProxy(MyInterface.class);
	}

}
