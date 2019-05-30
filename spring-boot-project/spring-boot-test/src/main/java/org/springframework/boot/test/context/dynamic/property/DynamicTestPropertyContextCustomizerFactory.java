package org.springframework.boot.test.context.dynamic.property;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;


/**
 * {@link ContextCustomizerFactory} to allow using the {@link DynamicTestProperty} in tests.
 *
 * @author Anatoliy Korovin
 */
public class DynamicTestPropertyContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> aClass, List<ContextConfigurationAttributes> list) {

		List<TestPropertyValues> properties = new ArrayList<>();

		for (Method m : aClass.getDeclaredMethods()) {

			if (m.isAnnotationPresent(DynamicTestProperty.class)) {

				if(!Modifier.isStatic(m.getModifiers())){
					throw new DynamicTestPropertyException("Annotation DynamicTestProperty must be used on a static method.");
				}

				if(!m.getReturnType().equals(TestPropertyValues.class)){
					throw new DynamicTestPropertyException("DynamicTestProperty method must return the instance of TestPropertyValues.");
				}

				try {
					m.setAccessible(true);
					Object result = m.invoke(null);
					properties.add((TestPropertyValues) result);
				} catch (Exception e) {
					throw new DynamicTestPropertyException("Error while trying to get a value of dynamic properties.");
				}
			}

		}

		return properties.isEmpty() ? null : new DynamicTestPropertyContextCustomizer(properties);
	}
}
