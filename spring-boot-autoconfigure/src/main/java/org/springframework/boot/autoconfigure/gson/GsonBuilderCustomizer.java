package org.springframework.boot.autoconfigure.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link Gson} via {@link GsonBuilder} retaining its default
 * auto-configuration.
 *
 * @author Ivan Golovko
 * @since 2.0.0
 */
@FunctionalInterface
public interface GsonBuilderCustomizer {

    /**
     * Customize the GsonBuilder.
     * @param gsonBuilder the GsonBuilder to customize
     */
    void customize(GsonBuilder gsonBuilder);
}
