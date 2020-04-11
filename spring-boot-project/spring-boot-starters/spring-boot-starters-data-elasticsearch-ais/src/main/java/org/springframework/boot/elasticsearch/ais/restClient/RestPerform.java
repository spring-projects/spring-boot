package org.springframework.boot.elasticsearch.ais.restClient;

import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
import java.util.List;
import org.elasticsearch.client.ResponseListener;
import org.springframework.stereotype.Component;


/**
 * .Executing scripts and parsing returned values
 *
 * @author lihang
 * @email 631533483@qq.com
 */
@Component
public interface RestPerform {

  /**
   * Executing scripts and parsing returned values.
   *
   * @param method   Request mode(get,put,post,delete)
   * @param endpoint url
   * @param script   JSON script
   * @return String，Need to resolve by yourself
   */
  String executeString(String method, String endpoint, String script) throws IOException;

  /**
   * Executing scripts and parsing returned values.
   *
   * @param method   Request mode(get,put,post,delete)
   * @param endpoint url
   * @param script   JSON script
   * @param tClass   Class corresponding to entity class
   * @param <T>      Objects in List
   * @return List，Need to resolve by yourself
   */
  <T> List<T> execute(String method, String endpoint, String script, Class<T> tClass)
      throws IOException;

  /**
   * Executing scripts and parsing returned values.
   *
   * @param method   Request mode(get,put,post,delete)
   * @param endpoint url
   * @param script   JSON script
   * @return JSON，Need to resolve by yourself
   */
  JSONObject executeJSON(String method, String endpoint, String script) throws IOException;

  /**
   * Executing scripts and parsing returned values.
   *
   * @param method   Request mode(get,put,post,delete)
   * @param endpoint url
   * @param script   JSON script
   * @param tClass   Class corresponding to entity class
   * @param <T>      Objects in List
   * @return List，Need to resolve by yourself
   */
  <T> T executeOne(String method, String endpoint, String script, Class<T> tClass)
      throws IOException;


  /**
   * Executing scripts and parsing returned values.
   *
   * @param method   Request mode(get,put,post,delete)
   * @param endpoint url
   * @param script   JSON script
   * @param tClass   Class corresponding to entity class
   * @param <T>      Objects in List
   * @return List，Need to resolve by yourself
   */
  <T> List<T> executeAsync(String method, String endpoint, String script, int num, Class<T> tClass)
      throws IOException, InterruptedException;

  /**
   * Executing scripts and parsing returned values.
   *
   * @param method   Request mode(get,put,post,delete)
   * @param endpoint url
   * @param script   JSON script
   */
  void executeAsync(String method, String endpoint, String script,
      ResponseListener responseListener)
      throws IOException;

  /**
   * Executing scripts and parsing returned values.
   *
   * @param method   Request mode(get,put,post,delete)
   * @param endpoint url
   * @param script   JSON script
   * @return JSON，Need to resolve by yourself
   */
  JSONObject executeJSONAsync(String method, String endpoint, String script, int num)
      throws IOException, InterruptedException;

  /**
   * Executing scripts and parsing returned values.
   *
   * @param method   Request mode(get,put,post,delete)
   * @param endpoint url
   * @param script   JSON script
   * @param tClass   Class corresponding to entity class
   * @param <T>      Objects in List
   * @return List，Need to resolve by yourself
   */
  <T> T executeOneAsync(String method, String endpoint, String script, int num, Class<T> tClass)
      throws IOException, InterruptedException;


}
