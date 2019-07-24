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

package org.springframework.boot.jta.bitronix;

import java.sql.Connection;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

import bitronix.tm.TransactionManagerServices;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
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
	public void sensibleDefaults() {
		assertThat(this.bean.getMaxPoolSize()).isEqualTo(10);
		assertThat(this.bean.getAutomaticEnlistingEnabled()).isTrue();
		assertThat(this.bean.isEnableJdbc4ConnectionTest()).isTrue();
	}

	@Test
	public void setsUniqueNameIfNull() throws Exception {
		this.bean.setBeanName("beanName");
		this.bean.afterPropertiesSet();
		assertThat(this.bean.getUniqueName()).isEqualTo("beanName");
	}

	@Test
	public void doesNotSetUniqueNameIfNotNull() throws Exception {
		this.bean.setBeanName("beanName");
		this.bean.setUniqueName("un");
		this.bean.afterPropertiesSet();
		assertThat(this.bean.getUniqueName()).isEqualTo("un");
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
		TransactionManagerServices.getTaskScheduler().shutdown();
	}

}
