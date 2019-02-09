/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.neo4j;

import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.neo4j.bookmark.BeanFactoryBookmarkOperationAdvisor;
import org.springframework.data.neo4j.bookmark.BookmarkInterceptor;
import org.springframework.data.neo4j.bookmark.BookmarkManager;
import org.springframework.data.neo4j.bookmark.CaffeineBookmarkManager;
import org.springframework.web.context.WebApplicationContext;

/**
 * Provides a {@link BookmarkManager} for Neo4j's bookmark support based on Caffeine if
 * available. Depending on the application's type (web or not) the bookmark manager will
 * be bound to the application or the request, as recommend by Spring Data Neo4j.
 *
 * @author Michael Simons
 */
@Configuration
@ConditionalOnClass({ Caffeine.class, CaffeineCacheManager.class })
@ConditionalOnMissingBean(BookmarkManager.class)
@ConditionalOnBean({ BeanFactoryBookmarkOperationAdvisor.class,
		BookmarkInterceptor.class })
class Neo4jBookmarkManagementConfiguration {

	private static final String BOOKMARK_MANAGER_BEAN_NAME = "bookmarkManager";

	@Bean(BOOKMARK_MANAGER_BEAN_NAME)
	@ConditionalOnWebApplication
	@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.INTERFACES)
	public BookmarkManager requestScopedBookmarkManager() {
		return new CaffeineBookmarkManager();
	}

	@Bean(BOOKMARK_MANAGER_BEAN_NAME)
	@ConditionalOnNotWebApplication
	public BookmarkManager singletonScopedBookmarkManager() {
		return new CaffeineBookmarkManager();
	}

}
