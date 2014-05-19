/*
 * Copyright 2003-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.autoconfigure.groovy.template;

import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import groovy.text.markup.TemplateResolver;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.IOException;
import java.net.URL;

/**
 * A custom {@link groovy.text.markup.TemplateResolver template resolver} which resolves templates using the locale
 * found in the thread locale. This resolver ignores the template engine configuration locale.
 *
 * @author Cédric Champeau
 * @since 1.1.0
 */

public class GroovyTemplateResolver implements TemplateResolver {
    private ClassLoader templateClassLoader;

    @Override
    public void configure(final ClassLoader templateClassLoader, final TemplateConfiguration configuration) {
        this.templateClassLoader = templateClassLoader;
    }

    @Override
    public URL resolveTemplate(final String templatePath) throws IOException {
        MarkupTemplateEngine.TemplateResource templateResource = MarkupTemplateEngine.TemplateResource.parse(templatePath);
        URL resource = templateClassLoader.getResource(templateResource.withLocale(LocaleContextHolder.getLocale().toString().replace("-", "_")).toString());
        if (resource == null) {
            // no resource found with the default locale, try without any locale
            resource = templateClassLoader.getResource(templateResource.withLocale(null).toString());
        }
        if (resource == null) {
            throw new IOException("Unable to load template:" + templatePath);
        }
        return resource;
    }
}
