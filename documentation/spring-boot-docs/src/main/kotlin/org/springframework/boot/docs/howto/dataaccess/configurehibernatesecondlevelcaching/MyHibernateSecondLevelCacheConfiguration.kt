/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.dataaccess.configurehibernatesecondlevelcaching

import org.hibernate.cache.jcache.ConfigSettings
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
import org.springframework.cache.jcache.JCacheCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyHibernateSecondLevelCacheConfiguration {

	@Bean
	fun hibernateSecondLevelCacheCustomizer(cacheManager: JCacheCacheManager): HibernatePropertiesCustomizer {
		return HibernatePropertiesCustomizer { properties ->
			val cacheManager = cacheManager.cacheManager
			if (cacheManager != null) {
				properties[ConfigSettings.CACHE_MANAGER] = cacheManager
			}
		}
	}

}

