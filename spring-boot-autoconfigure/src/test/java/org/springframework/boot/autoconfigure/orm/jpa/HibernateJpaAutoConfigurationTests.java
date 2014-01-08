/*
 * Copyright 2012-2013 the original author or authors.
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

import org.junit.Test;
import org.springframework.boot.test.SpringBootTestUtils;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link HibernateJpaAutoConfiguration}.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
public class HibernateJpaAutoConfigurationTests extends AbstractJpaAutoConfigurationTests {

	@Override
	protected Class<?> getAutoConfigureClass() {
		return HibernateJpaAutoConfiguration.class;
	}

	@Test
	public void testCustomNamingStrategy() throws Exception {
		SpringBootTestUtils.addEnviroment(this.context, "spring.jpa.hibernate.namingstrategy:"
				+ "org.hibernate.cfg.EJB3NamingStrategy");
		setupTestConfiguration();
		this.context.refresh();
		LocalContainerEntityManagerFactoryBean bean = this.context
				.getBean(LocalContainerEntityManagerFactoryBean.class);
		String actual = (String) bean.getJpaPropertyMap().get(
				"hibernate.ejb.naming_strategy");
		assertThat(actual, equalTo("org.hibernate.cfg.EJB3NamingStrategy"));
	}

}
