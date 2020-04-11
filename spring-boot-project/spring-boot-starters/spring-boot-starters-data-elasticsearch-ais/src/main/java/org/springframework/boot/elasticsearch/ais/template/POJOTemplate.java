package org.springframework.boot.elasticsearch.ais.template;

import com.alibaba.fastjson.JSONObject;
import org.springframework.boot.elasticsearch.ais.Estemplate;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Query template, inherit when using.
 *
 * @param <T> field entity
 * @author lihang
 * @email 631533483@qq.com
 */
@Component
public abstract class POJOTemplate<T> {

  @Autowired
  protected Estemplate estemplate;
  protected String index = "";

  public String getIndex() {
    return index;
  }

  public void setIndex(String index) {
    this.index = index;
  }

  public abstract void setIndex();

  public abstract void setConfig(T entity);


  public abstract void findRule(T entity);

  public void deleteRule(T entity) {
    this.findRule(entity);
  }

  public void updateRule(T entity) {
    this.findRule(entity);
  }

  /**
   * find.
   *
   * @param entity data
   * @return JSON
   */
  public JSONObject findJson(T entity) throws IOException {
    this.setIndex();
    this.setConfig(entity);
    this.findRule(entity);
    return estemplate.executeJSON("get", index + "/_search");
  }

  /**
   * find.
   *
   * @param entity data
   * @return List data
   */
  public List<T> find(T entity) throws IOException {
    this.setIndex();
    this.setConfig(entity);
    this.findRule(entity);
    return (List<T>) estemplate.execute("get", index + "/_search", entity.getClass());
  }

  /**
   * delete.
   *
   * @param entity data
   * @return JSON
   */
  public JSONObject delete(T entity) throws IOException {
    this.setIndex();
    this.deleteRule(entity);
    return estemplate.executeJSON("post", index + "/_delete_by_query");
  }

  /**
   * delete.
   *
   * @param id ids
   * @return JSON
   */
  public JSONObject delete(String... id) throws IOException {
    this.setIndex();
    return estemplate.delete(index, id);
  }

  /**
   * insert.
   *
   * @param entity data
   * @return JSON
   */
  public JSONObject insert(T entity) throws IOException {
    this.setIndex();
    return estemplate.insert(entity, index);
  }

  /**
   * insert.
   *
   * @param entityList list data
   * @return JSON
   */
  public JSONObject insert(List<T> entityList) throws IOException {
    this.setIndex();
    return estemplate.insert(entityList, index);
  }

  /**
   * insert.
   *
   * @param <K>   id
   * @param <V>   data
   * @param index index
   * @return JSON
   */
  public <K, V> JSONObject insert(Map<K, V> map, String index) throws IOException {
    this.setIndex();
    return estemplate.insert(map, index);
  }

  /**
   * insert.
   *
   * @param map    field name,field value
   * @param entity data
   * @return JSON
   */
  public <V> JSONObject update(T entity, Map<String, V> map) throws IOException {
    this.setIndex();
    this.updateRule(entity);
    return estemplate.update_by_query(map, index);
  }


}
