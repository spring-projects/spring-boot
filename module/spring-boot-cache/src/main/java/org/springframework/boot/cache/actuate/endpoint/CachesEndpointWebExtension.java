/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.actuate.endpoint;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.cache.actuate.endpoint.CachesEndpoint.CacheEntryDescriptor;

/**
 * {@link EndpointWebExtension @EndpointWebExtension} for the {@link CachesEndpoint}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@EndpointWebExtension(endpoint = CachesEndpoint.class)
public class CachesEndpointWebExtension {

	private final CachesEndpoint delegate;

	public CachesEndpointWebExtension(CachesEndpoint delegate) {
		this.delegate = delegate;
	}

	@ReadOperation
	public WebEndpointResponse<CacheEntryDescriptor> cache(@Selector String cache, @Nullable String cacheManager) {
		try {
			CacheEntryDescriptor entry = this.delegate.cache(cache, cacheManager);
			int status = (entry != null) ? WebEndpointResponse.STATUS_OK : WebEndpointResponse.STATUS_NOT_FOUND;
			return new WebEndpointResponse<>(entry, status);
		}
		catch (NonUniqueCacheException ex) {
			return new WebEndpointResponse<>(WebEndpointResponse.STATUS_BAD_REQUEST);
		}
	}

	@DeleteOperation
	public WebEndpointResponse<Void> clearCache(@Selector String cache, @Nullable String cacheManager) {
		try {
			boolean cleared = this.delegate.clearCache(cache, cacheManager);
			int status = (cleared ? WebEndpointResponse.STATUS_NO_CONTENT : WebEndpointResponse.STATUS_NOT_FOUND);
			return new WebEndpointResponse<>(status);
		}
		catch (NonUniqueCacheException ex) {
			return new WebEndpointResponse<>(WebEndpointResponse.STATUS_BAD_REQUEST);
		}
	}

}
