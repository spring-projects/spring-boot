/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.template;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.AbstractTemplateViewResolver;

/**
 * Base class for {@link ConfigurationProperties @ConfigurationProperties} of a
 * {@link AbstractTemplateViewResolver}.
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public abstract class AbstractTemplateViewResolverProperties extends AbstractViewResolverProperties {

	/**
	 * Prefix that gets prepended to view names when building a URL.
	 */
	private String prefix;

	/**
	 * Suffix that gets appended to view names when building a URL.
	 */
	private String suffix;

	/**
	 * Name of the RequestContext attribute for all views.
	 */
	private String requestContextAttribute;

	/**
	 * Whether all request attributes should be added to the model prior to merging with
	 * the template.
	 */
	private boolean exposeRequestAttributes = false;

	/**
	 * Whether all HttpSession attributes should be added to the model prior to merging
	 * with the template.
	 */
	private boolean exposeSessionAttributes = false;

	/**
	 * Whether HttpServletRequest attributes are allowed to override (hide) controller
	 * generated model attributes of the same name.
	 */
	private boolean allowRequestOverride = false;

	/**
	 * Whether to expose a RequestContext for use by Spring's macro library, under the
	 * name "springMacroRequestContext".
	 */
	private boolean exposeSpringMacroHelpers = true;

	/**
	 * Whether HttpSession attributes are allowed to override (hide) controller generated
	 * model attributes of the same name.
	 */
	private boolean allowSessionOverride = false;

	/**
     * Constructs a new AbstractTemplateViewResolverProperties object with the specified default prefix and suffix.
     *
     * @param defaultPrefix the default prefix to be used for view names
     * @param defaultSuffix the default suffix to be used for view names
     */
    protected AbstractTemplateViewResolverProperties(String defaultPrefix, String defaultSuffix) {
		this.prefix = defaultPrefix;
		this.suffix = defaultSuffix;
	}

	/**
     * Returns the prefix used for view names.
     * 
     * @return the prefix used for view names
     */
    public String getPrefix() {
		return this.prefix;
	}

	/**
     * Sets the prefix for the view names.
     * 
     * @param prefix the prefix to be set
     */
    public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
     * Returns the suffix used for resolving view names.
     * 
     * @return the suffix used for resolving view names
     */
    public String getSuffix() {
		return this.suffix;
	}

	/**
     * Sets the suffix for the view names.
     * 
     * @param suffix the suffix to be set
     */
    public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	/**
     * Returns the value of the requestContextAttribute property.
     *
     * @return the value of the requestContextAttribute property
     */
    public String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	/**
     * Sets the request context attribute for this view resolver.
     * 
     * @param requestContextAttribute the name of the request context attribute
     */
    public void setRequestContextAttribute(String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
     * Returns a boolean value indicating whether the request attributes should be exposed.
     * 
     * @return {@code true} if the request attributes should be exposed, {@code false} otherwise
     */
    public boolean isExposeRequestAttributes() {
		return this.exposeRequestAttributes;
	}

	/**
     * Sets whether to expose request attributes.
     * 
     * @param exposeRequestAttributes
     *            a boolean value indicating whether to expose request attributes
     */
    public void setExposeRequestAttributes(boolean exposeRequestAttributes) {
		this.exposeRequestAttributes = exposeRequestAttributes;
	}

	/**
     * Returns a boolean value indicating whether session attributes are exposed.
     * 
     * @return {@code true} if session attributes are exposed, {@code false} otherwise
     */
    public boolean isExposeSessionAttributes() {
		return this.exposeSessionAttributes;
	}

	/**
     * Sets the flag indicating whether to expose session attributes to the view.
     * 
     * @param exposeSessionAttributes the flag indicating whether to expose session attributes
     */
    public void setExposeSessionAttributes(boolean exposeSessionAttributes) {
		this.exposeSessionAttributes = exposeSessionAttributes;
	}

	/**
     * Returns a boolean value indicating whether request override is allowed.
     * 
     * @return true if request override is allowed, false otherwise
     */
    public boolean isAllowRequestOverride() {
		return this.allowRequestOverride;
	}

	/**
     * Sets the flag indicating whether to allow request override.
     * 
     * @param allowRequestOverride the flag indicating whether to allow request override
     */
    public void setAllowRequestOverride(boolean allowRequestOverride) {
		this.allowRequestOverride = allowRequestOverride;
	}

	/**
     * Returns a boolean value indicating whether session override is allowed.
     * 
     * @return {@code true} if session override is allowed, {@code false} otherwise
     */
    public boolean isAllowSessionOverride() {
		return this.allowSessionOverride;
	}

	/**
     * Sets whether session override is allowed.
     * 
     * @param allowSessionOverride
     *            true if session override is allowed, false otherwise
     */
    public void setAllowSessionOverride(boolean allowSessionOverride) {
		this.allowSessionOverride = allowSessionOverride;
	}

	/**
     * Returns the value indicating whether the Spring macro helpers are exposed.
     * 
     * @return {@code true} if the Spring macro helpers are exposed, {@code false} otherwise
     */
    public boolean isExposeSpringMacroHelpers() {
		return this.exposeSpringMacroHelpers;
	}

	/**
     * Sets the flag indicating whether to expose Spring macro helpers.
     * 
     * @param exposeSpringMacroHelpers the flag indicating whether to expose Spring macro helpers
     */
    public void setExposeSpringMacroHelpers(boolean exposeSpringMacroHelpers) {
		this.exposeSpringMacroHelpers = exposeSpringMacroHelpers;
	}

	/**
	 * Apply the given properties to a {@link AbstractTemplateViewResolver}. Use Object in
	 * signature to avoid runtime dependency on MVC, which means that the template engine
	 * can be used in a non-web application.
	 * @param viewResolver the resolver to apply the properties to.
	 */
	public void applyToMvcViewResolver(Object viewResolver) {
		Assert.isInstanceOf(AbstractTemplateViewResolver.class, viewResolver,
				() -> "ViewResolver is not an instance of AbstractTemplateViewResolver :" + viewResolver);
		AbstractTemplateViewResolver resolver = (AbstractTemplateViewResolver) viewResolver;
		resolver.setPrefix(getPrefix());
		resolver.setSuffix(getSuffix());
		resolver.setCache(isCache());
		if (getContentType() != null) {
			resolver.setContentType(getContentType().toString());
		}
		resolver.setViewNames(getViewNames());
		resolver.setExposeRequestAttributes(isExposeRequestAttributes());
		resolver.setAllowRequestOverride(isAllowRequestOverride());
		resolver.setAllowSessionOverride(isAllowSessionOverride());
		resolver.setExposeSessionAttributes(isExposeSessionAttributes());
		resolver.setExposeSpringMacroHelpers(isExposeSpringMacroHelpers());
		resolver.setRequestContextAttribute(getRequestContextAttribute());
		// The resolver usually acts as a fallback resolver (e.g. like a
		// InternalResourceViewResolver) so it needs to have low precedence
		resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 5);
	}

}
