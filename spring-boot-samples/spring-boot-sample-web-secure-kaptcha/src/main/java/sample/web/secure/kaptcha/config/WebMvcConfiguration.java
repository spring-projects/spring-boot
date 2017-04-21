package sample.web.secure.kaptcha.config;


import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import sample.web.secure.kaptcha.Application;

import java.util.Properties;

@Configuration
public class WebMvcConfiguration extends WebMvcConfigurerAdapter {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
    }

    @Bean
    public DefaultKaptcha captchaProducer() {
        Properties properties = new Properties();
        properties.put("kaptcha.border", "yes");
        properties.put("kaptcha.border.color", "lightGray");
        properties.put("kaptcha.textproducer.font.color", "darkGray");
        properties.put("kaptcha.image.width", "160");
        properties.put("kaptcha.image.height", "50");
        properties.put("kaptcha.textproducer.font.size", "40");
        properties.put("kaptcha.session.key", "kaptcha");
        properties.put("kaptcha.textproducer.char.length", "4");
        properties.put("kaptcha.background.clear.to", "gray");
        properties.put("kaptcha.textproducer.font.names","宋体,楷体,微软雅黑");

        Config config = new Config(properties);
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        kaptcha.setConfig(config);

        return kaptcha;
    }
}
