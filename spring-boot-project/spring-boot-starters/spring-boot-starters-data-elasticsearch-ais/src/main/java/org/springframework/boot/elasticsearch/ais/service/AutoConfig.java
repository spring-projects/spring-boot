package org.springframework.boot.elasticsearch.ais.service;


import org.springframework.boot.elasticsearch.ais.Estemplate;
import org.springframework.boot.elasticsearch.ais.EstemplateCustom;
import org.springframework.boot.elasticsearch.ais.config.Config;
import org.springframework.boot.elasticsearch.ais.config.EsClient;
import org.springframework.boot.elasticsearch.ais.restClient.RestPerform;
import org.springframework.boot.elasticsearch.ais.restClient.RestPerformIpml;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;


/**
 * register spring bean.
 *
 * @author lihang
 * @email 631533483@qq.com
 */
@Configuration
@ConditionalOnWebApplication
@EnableConfigurationProperties(Config.class)
public class AutoConfig {

  private Config config;

  public AutoConfig(Config config) {
    this.config = Config.getConfig();
  }

  @Bean(name = "config")
  @ConditionalOnMissingBean
  public Config config() {
    return config;
  }


  @Bean(name = "restPerform")
  @DependsOn(value = {"config"})
  @ConditionalOnMissingBean
  public RestPerform restPerform() {
    RestPerform restPerform = new RestPerformIpml(new EsClient(Config.getConfig()));
    return restPerform;
  }

  @Bean(name = "esClient")
  @DependsOn(value = {"config"})
  @ConditionalOnMissingBean
  public EsClient esClient() {
    return new EsClient(Config.getConfig());
  }

  @Bean
  @DependsOn(value = {"restPerform"})
  @ConditionalOnMissingBean
  public Estemplate estemplate() {
    return new Estemplate();
  }

  @Bean(name = "estemplateCustom")
  @DependsOn(value = {"restPerform"})
  @ConditionalOnMissingBean
  public EstemplateCustom estemplateCustom() {
    return new EstemplateCustom();
  }


}
