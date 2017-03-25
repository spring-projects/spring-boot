/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import javax.sql.DataSource;

import com.p6spy.engine.spy.P6DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ProxyDataSourceUtil}.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.5.3
 */
public class ProxyDataSourceUtilTests {

	@Test
	public void testExtractingRealDataSourceFromKnownProxyDataSource() throws Exception {
		DataSource realDataSource = mock(DataSource.class);
		DataSource proxyDataSource = new P6DataSource(realDataSource);

		DataSource dataSource = ProxyDataSourceUtil.tryFindRealDataSource(proxyDataSource);
		assertThat(dataSource).isSameAs(realDataSource);
	}

	@Test
	public void testExtractingRealDataSourceFromMultipleKnownProxyDataSource() throws Exception {
		DataSource realDataSource = mock(DataSource.class);
		DataSource proxyDataSource = new ProxyDataSource(new P6DataSource(realDataSource));

		DataSource dataSource = ProxyDataSourceUtil.tryFindRealDataSource(proxyDataSource);
		assertThat(dataSource).isSameAs(realDataSource);
	}

	@Test
	public void testExtractingRealDataSourceFromCustomProxyDataSourceFails() throws Exception {
		DataSource realDataSource = mock(DataSource.class);
		DataSource proxyDataSource = new CustomDataSourceProxy(realDataSource);

		DataSource dataSource = ProxyDataSourceUtil.tryFindRealDataSource(proxyDataSource);
		assertThat(dataSource).isSameAs(proxyDataSource);
	}
}
