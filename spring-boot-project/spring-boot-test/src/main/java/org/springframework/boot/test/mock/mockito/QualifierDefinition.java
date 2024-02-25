/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.MergedAnnotations;

/**
 * Definition of a Spring {@link Qualifier @Qualifier}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see Definition
 */
class QualifierDefinition {

	private final Field field;

	private final DependencyDescriptor descriptor;

	private final Set<Annotation> annotations;

	/**
     * Constructs a new QualifierDefinition object with the given field and annotations.
     * 
     * @param field the field for which the qualifier definition is being created
     * @param annotations the set of annotations associated with the field
     */
    QualifierDefinition(Field field, Set<Annotation> annotations) {
		// We can't use the field or descriptor as part of the context key
		// but we can assume that if two fields have the same qualifiers then
		// it's safe for Spring to use either for qualifier logic
		this.field = field;
		this.descriptor = new DependencyDescriptor(field, true);
		this.annotations = annotations;
	}

	/**
     * Checks if the specified bean in the given bean factory matches the autowire candidate criteria.
     * 
     * @param beanFactory the bean factory to check against
     * @param beanName the name of the bean to check
     * @return true if the bean matches the autowire candidate criteria, false otherwise
     */
    boolean matches(ConfigurableListableBeanFactory beanFactory, String beanName) {
		return beanFactory.isAutowireCandidate(beanName, this.descriptor);
	}

	/**
     * Applies the qualified element to the given RootBeanDefinition.
     * 
     * @param definition the RootBeanDefinition to apply the qualified element to
     */
    void applyTo(RootBeanDefinition definition) {
		definition.setQualifiedElement(this.field);
	}

	/**
     * Compares this QualifierDefinition object with the specified object for equality.
     * 
     * @param obj the object to compare with
     * @return true if the specified object is equal to this QualifierDefinition object, false otherwise
     */
    @Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || !getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		QualifierDefinition other = (QualifierDefinition) obj;
		return this.annotations.equals(other.annotations);
	}

	/**
     * Returns the hash code value for this QualifierDefinition object.
     * 
     * @return the hash code value for this object
     */
    @Override
	public int hashCode() {
		return this.annotations.hashCode();
	}

	/**
     * Returns the QualifierDefinition for the given AnnotatedElement.
     * 
     * @param element the AnnotatedElement to get the QualifierDefinition for
     * @return the QualifierDefinition for the given element, or null if the element is null or not an instance of Field or if no qualifier annotations are found
     */
    static QualifierDefinition forElement(AnnotatedElement element) {
		if (element != null && element instanceof Field field) {
			Set<Annotation> annotations = getQualifierAnnotations(field);
			if (!annotations.isEmpty()) {
				return new QualifierDefinition(field, annotations);
			}
		}
		return null;
	}

	/**
     * Returns a set of qualifier annotations for the given field.
     * 
     * @param field the field for which to retrieve qualifier annotations
     * @return a set of qualifier annotations
     */
    private static Set<Annotation> getQualifierAnnotations(Field field) {
		// Assume that any annotations other than @MockBean/@SpyBean are qualifiers
		Annotation[] candidates = field.getDeclaredAnnotations();
		Set<Annotation> annotations = new HashSet<>(candidates.length);
		for (Annotation candidate : candidates) {
			if (!isMockOrSpyAnnotation(candidate.annotationType())) {
				annotations.add(candidate);
			}
		}
		return annotations;
	}

	/**
     * Checks if the given annotation type is either {@link MockBean} or {@link SpyBean},
     * or if it is present as a meta-annotation on the provided class.
     * 
     * @param type the annotation type to check
     * @return {@code true} if the annotation type is either {@link MockBean} or {@link SpyBean},
     *         or if it is present as a meta-annotation on the provided class; {@code false} otherwise
     */
    private static boolean isMockOrSpyAnnotation(Class<? extends Annotation> type) {
		if (type.equals(MockBean.class) || type.equals(SpyBean.class)) {
			return true;
		}
		MergedAnnotations metaAnnotations = MergedAnnotations.from(type);
		return metaAnnotations.isPresent(MockBean.class) || metaAnnotations.isPresent(SpyBean.class);
	}

}
