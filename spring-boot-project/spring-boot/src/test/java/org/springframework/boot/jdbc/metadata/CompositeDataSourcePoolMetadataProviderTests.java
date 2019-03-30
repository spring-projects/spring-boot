/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.jdbc.metadata;

import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link CompositeDataSourcePoolMetadataProvider}.
 *
 * @author Stephane Nicoll
 */
public class CompositeDataSourcePoolMetadataProviderTests {

	@Mock
	private DataSourcePoolMetadataProvider firstProvider;

	@Mock
	private DataSourcePoolMetadata first;

	@Mock
	private DataSource firstDataSource;

	@Mock
	private DataSourcePoolMetadataProvider secondProvider;

	@Mock
	private DataSourcePoolMetadata second;

	@Mock
	private DataSource secondDataSource;

	@Mock
	private DataSource unknownDataSource;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		given(this.firstProvider.getDataSourcePoolMetadata(this.firstDataSource))
				.willReturn(this.first);
		given(this.firstProvider.getDataSourcePoolMetadata(this.secondDataSource))
				.willReturn(this.second);
	}

	@Test
	public void createWithProviders() {
		CompositeDataSourcePoolMetadataProvider provider = new CompositeDataSourcePoolMetadataProvider(
				Arrays.asList(this.firstProvider, this.secondProvider));
		assertThat(provider.getDataSourcePoolMetadata(this.firstDataSource))
				.isSameAs(this.first);
		assertThat(provider.getDataSourcePoolMetadata(this.secondDataSource))
				.isSameAs(this.second);
		assertThat(provider.getDataSourcePoolMetadata(this.unknownDataSource)).isNull();
	}

}
