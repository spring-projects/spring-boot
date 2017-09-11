/**
 * Copyright 2012-2017 the original author or authors.
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
package org.springframework.boot.actuate.metrics.web;

import org.springframework.core.NamedThreadLocal;

/**
 * Holding area for the still-templated URI because currently the
 * ClientHttpRequestInterceptor only gives us the means to retrieve the substituted URI.
 *
 * @since 2.0.0
 * @author Jon Schneider
 */
public class RestTemplateUrlTemplateHolder {
    private static final ThreadLocal<String> restTemplateUrlTemplateHolder = new NamedThreadLocal<String>(
            "Rest Template URL Template");

    public static String getRestTemplateUrlTemplate() {
        return restTemplateUrlTemplateHolder.get();
    }

    public static void setRestTemplateUrlTemplate(String urlTemplate) {
        restTemplateUrlTemplateHolder.set(urlTemplate);
    }

    public static void clear() {
        restTemplateUrlTemplateHolder.remove();
    }
}
