package org.springframework.boot.autoconfigure.ehcache;

import net.sf.ehcache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for EhCache.
 *
 * @author Eddú Meléndez
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass({CacheManager.class, org.springframework.cache.CacheManager.class,
	EhCacheCacheManager.class})
public class EhCacheAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(org.springframework.cache.CacheManager.class)
	@EnableConfigurationProperties(EhCacheProperties.class)
	public static class EhCacheManagerFactoryConfiguration {

		@Autowired
		private EhCacheProperties properties;

		@Bean
		@ConditionalOnMissingBean
		public org.springframework.cache.CacheManager ehCacheManager() {
			return new EhCacheCacheManager(EhCacheManagerUtils
				.buildCacheManager(properties.getName(),
					new ClassPathResource(properties.getLocation())));
		}

	}

}
