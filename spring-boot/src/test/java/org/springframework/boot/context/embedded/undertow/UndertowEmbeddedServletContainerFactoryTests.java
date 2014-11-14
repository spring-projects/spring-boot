package org.springframework.boot.context.embedded.undertow;
import org.junit.Test;
import org.springframework.boot.context.embedded.*;
import org.springframework.http.HttpStatus;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;


/**
 * Tests for {@link org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory} and
 * {@link org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainer}.
 *
 * @author Ivan Sopov
 */
public class UndertowEmbeddedServletContainerFactoryTests extends AbstractEmbeddedServletContainerFactoryTests
{

    @Override
    protected UndertowEmbeddedServletContainerFactory getFactory()
    {
        return new UndertowEmbeddedServletContainerFactory();
    }

    @Test
    public void errorPage404() throws Exception {
        AbstractEmbeddedServletContainerFactory factory = getFactory();
        factory.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/hello"));
        this.container = factory.getEmbeddedServletContainer(new ServletRegistrationBean(new ExampleServlet(), "/hello"));
        this.container.start();
        assertThat(getResponse("http://localhost:8080/hello"), equalTo("Hello World"));
        assertThat(getResponse("http://localhost:8080/not-found"), equalTo("Hello World"));
    }
}
