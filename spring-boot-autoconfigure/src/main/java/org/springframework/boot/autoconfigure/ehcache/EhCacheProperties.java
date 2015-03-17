package org.springframework.boot.autoconfigure.ehcache;

import net.sf.ehcache.CacheManager;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotNull;

/**
 * Configuration properties to configure {@link org.springframework.cache.ehcache.EhCacheFactoryBean}.
 *
 * @author Eddú Meléndez
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.cache.ehcache",
	ignoreUnknownFields = false)
public class EhCacheProperties {

	/**
	 * Location of ehcache config.
	 */
	@NotNull
	private String location = "ehcache.xml";

	/**
	 * Name of the CacheManager.
	 */
	private String name = CacheManager.DEFAULT_NAME;

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
