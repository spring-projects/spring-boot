/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.test.context.PropertyMapping;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;

/**
 * Annotation that can be applied to a test class to customize the
 * {@linkplain #cacheProvider() cache provider}. By default, a {@link NoOpCacheManager} is
 * auto-configured
 * <p>
 * As for the regular auto-configuration, this has no effect if a custom
 * {@link CacheManager} is defined.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
public @interface AutoConfigureCache {

	/**
	 * The {@link CacheType} to configure, overriding the behavior for testing purposes.
	 * @return the cache type to configure
	 */
	@PropertyMapping("spring.cache.type")
	CacheType cacheProvider() default CacheType.NONE;

}
