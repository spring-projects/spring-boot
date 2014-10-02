package sample.jersey1;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.sun.jersey.spi.container.servlet.ServletContainer;

@Configuration
@ComponentScan
@EnableAutoConfiguration
@Path("/")
public class SampleJersey1Application {

    public static void main(String[] args) {
       new SpringApplicationBuilder(SampleJersey1Application.class).web(true).run(args);
    }
    
    @GET
    @Produces("text/plain")
    public String hello() {
    	return "Hello World";
    }
    
    @Bean
    // Not needed if Spring Web MVC is also present on claspath 
    public TomcatEmbeddedServletContainerFactory tomcatEmbeddedServletContainerFactory() {
    	return new TomcatEmbeddedServletContainerFactory();
    }
    
    @Bean
    public FilterRegistrationBean jersey() {
    	FilterRegistrationBean bean = new FilterRegistrationBean();
    	bean.setFilter(new ServletContainer());
    	bean.addInitParameter("com.sun.jersey.config.property.packages", "com.sun.jersey;demo");
		return bean;
    }
    
}
