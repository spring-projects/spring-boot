package org.springframework.boot.elasticsearch.ais.service;

import org.springframework.boot.elasticsearch.ais.Factory.EsAsyncFactory;
import org.springframework.boot.elasticsearch.ais.annotation.ElasticsearchAsync;
import org.springframework.boot.elasticsearch.ais.config.Scanner;
import org.springframework.boot.elasticsearch.ais.utils.StringUtils;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;

/**
 * <p>register spring bean,Register as class name, first level letter is lowercase.</p>
 *
 * @author lihang
 * @email 631533483@qq.com
 */
@ConditionalOnWebApplication
public class EsAsyncRegistryBean implements ApplicationContextAware,
    BeanDefinitionRegistryPostProcessor {

  private ApplicationContext applicationContext;
  private Set<Class<?>> set = new HashSet<>();
  Logger log = LoggerFactory.getLogger(ElasticsearchAsync.class);

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void postProcessBeanFactory(
      ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

  }

  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry)
      throws BeansException {
    try {
      this.setSet();
    } catch (Exception e) {
      log.error("Failed to load bean, please reload or check for errors\n");
      log.error(e.toString());
    }
    if (set.isEmpty()) {
      log.info("Interface without dynamic agent\n");
    } else {
      for (Class<?> c : set) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(c);
        GenericBeanDefinition definition = (GenericBeanDefinition) builder.getRawBeanDefinition();
        definition.getPropertyValues().add("interfaceClass", definition.getBeanClassName());
        definition.setBeanClass(EsAsyncFactory.class);
        definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
        // 注册bean名,一般为类名首字母小写
        String name = c.getSimpleName();
        name = name.substring(0, 1).toLowerCase() + name.substring(1, name.length());
        beanDefinitionRegistry.registerBeanDefinition(name, definition);
        log.info(c.getSimpleName() + " registry as " + name);
      }
    }
  }

  private void setSet() throws Exception {
    ClassPathResource classPathResource1 = new ClassPathResource("application.yml");
    ClassPathResource classPathResource2 = new ClassPathResource("application.properties");
    ClassPathResource classPathResource3 = new ClassPathResource("bootstap.yml");
    ClassPathResource classPathResource4 = new ClassPathResource("bootstap.properties");
    Properties pros = new Properties();
    String path = null;
    InputStream inputStream;
    if (classPathResource3.exists()) {
      inputStream = classPathResource3.getInputStream();
      pros.load(inputStream);
      path = pros.getProperty("aisResource");
      if (StringUtils.isNotBlank(path)) {
        set = new Scanner().getAnnotationClasses(path, ElasticsearchAsync.class);
      }
    } else if (classPathResource1.exists()) {
      inputStream = classPathResource1.getInputStream();
      pros.load(inputStream);
      path = pros.getProperty("aisResource");
      if (StringUtils.isNotBlank(path)) {
        set = new Scanner().getAnnotationClasses(path, ElasticsearchAsync.class);
      }
    } else if (classPathResource4.exists()) {
      inputStream = classPathResource4.getInputStream();
      pros.load(inputStream);
      path = pros.getProperty("elasticsearch.ais.aisResource");
      if (StringUtils.isNotBlank(path)) {
        set = new Scanner().getAnnotationClasses(path, ElasticsearchAsync.class);
      }
    } else if (classPathResource2.exists()) {
      inputStream = classPathResource2.getInputStream();
      pros.load(inputStream);
      path = pros.getProperty("elasticsearch.ais.aisResource");
      if (StringUtils.isNotBlank(path)) {
        set = new Scanner().getAnnotationClasses(path, ElasticsearchAsync.class);
      }
    } else {
      set = new Scanner().getAnnotationClasses("com", ElasticsearchAsync.class);
    }
  }


}

