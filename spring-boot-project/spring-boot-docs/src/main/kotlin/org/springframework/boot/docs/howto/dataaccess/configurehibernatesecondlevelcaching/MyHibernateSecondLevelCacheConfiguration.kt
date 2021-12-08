package org.springframework.boot.docs.howto.dataaccess.configurehibernatesecondlevelcaching

import org.hibernate.cache.jcache.ConfigSettings
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.cache.jcache.JCacheCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyHibernateSecondLevelCacheConfiguration {
	@Bean
	fun hibernateSecondLevelCacheCustomizer(cacheManager: JCacheCacheManager): HibernatePropertiesCustomizer {
		return HibernatePropertiesCustomizer { properties: MutableMap<String?, Any?> ->
			properties[ConfigSettings.CACHE_MANAGER] = cacheManager.cacheManager
		}
	}
}