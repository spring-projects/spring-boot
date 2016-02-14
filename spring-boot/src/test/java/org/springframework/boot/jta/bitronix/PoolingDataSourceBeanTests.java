/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.jta.bitronix;

import java.sql.Connection;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link PoolingDataSourceBean}.
 *
 * @author Phillip Webb
 */
public class PoolingDataSourceBeanTests {

	private PoolingDataSourceBean bean = new PoolingDataSourceBean();

	@Test
	public void sensibleDefaults() throws Exception {
		assertThat(this.bean.getMaxPoolSize(), equalTo(10));
		assertThat(this.bean.getAutomaticEnlistingEnabled(), equalTo(true));
		assertThat(this.bean.isEnableJdbc4ConnectionTest(), equalTo(true));
	}

	@Test
	public void setsUniqueNameIfNull() throws Exception {
		this.bean.setBeanName("beanName");
		this.bean.afterPropertiesSet();
		assertThat(this.bean.getUniqueName(), equalTo("beanName"));
	}

	@Test
	public void doesNotSetUniqueNameIfNotNull() throws Exception {
		this.bean.setBeanName("beanName");
		this.bean.setUniqueName("un");
		this.bean.afterPropertiesSet();
		assertThat(this.bean.getUniqueName(), equalTo("un"));
	}

	@Test
	public void setDataSource() throws Exception {
		XADataSource dataSource = mock(XADataSource.class);
		XAConnection xaConnection = mock(XAConnection.class);
		Connection connection = mock(Connection.class);
		given(dataSource.getXAConnection()).willReturn(xaConnection);
		given(xaConnection.getConnection()).willReturn(connection);
		this.bean.setDataSource(dataSource);
		this.bean.setBeanName("beanName");
		this.bean.afterPropertiesSet();
		this.bean.init();
		this.bean.createPooledConnection(dataSource, this.bean);
		verify(dataSource).getXAConnection();
	}

}
