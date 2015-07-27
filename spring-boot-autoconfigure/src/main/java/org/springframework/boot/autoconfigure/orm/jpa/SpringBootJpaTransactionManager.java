/*
 * Copyright 2012-2015 the original author or authors.
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
package org.springframework.boot.autoconfigure.orm.jpa;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Spring Boot customization of JpaTransactionManager to apply configured default isolation level (see {@link JpaProperties}).
 * 
 * @author Arnost Havelka
 */
@SuppressWarnings("serial")
public class SpringBootJpaTransactionManager extends JpaTransactionManager {

	private boolean customizeIsolationLevel = false; 
	
	private int isolationLevel = TransactionDefinition.ISOLATION_DEFAULT;

	public SpringBootJpaTransactionManager(JpaProperties jpaProperties) {
		// verify definition JPA properties
		if (jpaProperties == null) {
			return;
		}
		// define customization properties
		isolationLevel = jpaProperties.getDefaultIsolationLevel();
		customizeIsolationLevel = TransactionDefinition.ISOLATION_DEFAULT != isolationLevel;
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.orm.jpa.JpaTransactionManager#doBegin(java.lang.Object, org.springframework.transaction.TransactionDefinition)
	 */
	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		if (customizeIsolationLevel && definition.getIsolationLevel() == TransactionDefinition.ISOLATION_DEFAULT) {
			// override isolation level (as configured)
			DefaultTransactionDefinition dtd = new DefaultTransactionDefinition(definition);
			dtd.setIsolationLevel(isolationLevel);
			// proceed with configured isolation level
			super.doBegin(transaction, dtd);
		} else {
			// proceed with default/original isolation level
			super.doBegin(transaction, definition);
		}
		
	}
}
