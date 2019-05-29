package org.springframework.boot.test.context.dynamic.property;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class DynamicTestPropertyContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> aClass, List<ContextConfigurationAttributes> list) {

		List<TestPropertyValues> properties = new ArrayList<>();

		for (Method m : aClass.getDeclaredMethods()) {

			if (m.isAnnotationPresent(DynamicTestProperty.class)) {
				try {
					m.setAccessible(true);
					Object result = m.invoke(null);
					properties.add((TestPropertyValues) result);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}

		}

		return properties.isEmpty() ? null : new DynamicTestPropertyContextCustomizer(properties);
	}
}
