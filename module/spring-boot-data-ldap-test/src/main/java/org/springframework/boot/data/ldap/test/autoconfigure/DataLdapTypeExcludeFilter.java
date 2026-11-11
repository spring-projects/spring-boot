/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.ldap.test.autoconfigure;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * {@link TypeExcludeFilter} for {@link DataLdapTest @DataLdapTest}.
 *
 * @author Eddú Meléndez
 */
class DataLdapTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<DataLdapTest> {

	DataLdapTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

}
