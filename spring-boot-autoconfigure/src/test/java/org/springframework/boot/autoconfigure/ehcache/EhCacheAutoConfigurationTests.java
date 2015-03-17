package org.springframework.boot.autoconfigure.ehcache;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link org.springframework.boot.autoconfigure.ehcache.EhCacheAutoConfiguration}.
 *
 * @author Eddú Meléndez
 */
public class EhCacheAutoConfigurationTests {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@Before
	public void init() {
		context = new AnnotationConfigApplicationContext();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultEhCache() {
		load();
		EhCacheCacheManager cacheManager = this.context.getBean(EhCacheCacheManager.class);
		assertEquals(1, this.context.getBeanNamesForType(CacheManager.class).length);
		assertEquals(cacheManager.getCacheManager().getName(),
			net.sf.ehcache.CacheManager.DEFAULT_NAME);
		assertThat(cacheManager.getCacheNames(), contains("rootCache"));
	}

	@Test
	public void testOverrideEhCacheName() {
		load("spring.cache.ehcache.name:cacheTest");
		EhCacheCacheManager cacheManager = this.context.getBean(EhCacheCacheManager.class);
		assertEquals(cacheManager.getCacheManager().getName(), "cacheTest");
		assertThat(cacheManager.getCacheNames(), contains("rootCache"));
	}

	@Test
	public void testOverrideEhCacheLocationAndEhCacheName() {
		load("spring.cache.ehcache.name:cacheTest2",
			"spring.cache.ehcache.location:ehcache/ehcache-override.xml");
		EhCacheCacheManager cacheManager = this.context.getBean(EhCacheCacheManager.class);
		assertEquals(cacheManager.getCacheManager().getName(), "cacheTest2");
		assertThat(cacheManager.getCacheNames(), contains("spittleCache"));
	}

	@Test
	public void testEhCacheLocationDoesNotExist() {
		expectedException.expect(BeanCreationException.class);
		expectedException.expectMessage("Failed to parse EhCache configuration resource");
		load("spring.cache.ehcache.location:ehcache-test.xml");
	}

	private void load(String... environment) {
		this.context = doLoad(environment);
	}

	private AnnotationConfigApplicationContext doLoad(String... environment) {
		AnnotationConfigApplicationContext applicationContext =
			new AnnotationConfigApplicationContext();
		applicationContext
			.register(EhCacheAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.refresh();
		return applicationContext;
	}

}
