package sample.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.ws.transport.http.MessageDispatcherServlet;

/**
 * Created by in329dei on 28-2-14.
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan
@ImportResource("classpath:/META-INF/spring/spring-ws-context.xml")
public class SampleWsApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(SampleWsApplication.class, args);
    }

    @Bean
    public ServletRegistrationBean messageDispatcherServletRegistration() {
        MessageDispatcherServlet mds = new MessageDispatcherServlet();
        mds.setTransformWsdlLocations(true);

        ServletRegistrationBean srb = new ServletRegistrationBean(messageDispatcherServlet(), "/services/*");
        srb.setLoadOnStartup(1);
        return srb;
    }

    @Bean
    public MessageDispatcherServlet messageDispatcherServlet() {
        MessageDispatcherServlet mds = new MessageDispatcherServlet();
        mds.setTransformWsdlLocations(true);
        return mds;
    }
}
