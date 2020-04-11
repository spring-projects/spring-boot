package org.springframework.boot.elasticsearch.ais.Factory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * Dynamic proxy with Elasticsearch annotation interface(Spring).
 *
 * @author lihang
 * @email 631533483@qq.com
 */
@Component
public class EsFactory<T> implements FactoryBean<T> {

  private Class<T> interfaceClass;

  public Class<T> getInterfaceClass() {
    return interfaceClass;
  }

  public void setInterfaceClass(Class<T> interfaceClass) {
    this.interfaceClass = interfaceClass;
  }

  @Override
  public T getObject() throws Exception {
    return (T) new EsProxy().bind(interfaceClass);
  }

  @Override
  public Class<?> getObjectType() {
    return interfaceClass;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }
}
