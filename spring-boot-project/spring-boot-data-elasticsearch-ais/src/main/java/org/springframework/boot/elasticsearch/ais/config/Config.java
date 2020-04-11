package org.springframework.boot.elasticsearch.ais.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Profile content acquisition.
 *
 * @author lihang
 * @email 631533483@qq.com
 */
@Component
@ConfigurationProperties("elasticsearch.ais")
public class Config {

  private static Config config = new Config();

  //private Config(){}
  public static Config getConfig() {
    return config;
  }

  private String url = "localhost:9200"; //端口号
  private String userName;
  private String passWard;
  private int socketTimeout = 5000;
  private int connectTimeout = 10000;
  private String scheme = "http";
  private String header = "";
  private String value = "";
  private String jsonPath = "";
  private String aisResource;

  public int getSocketTimeout() {
    return socketTimeout;
  }

  public void setSocketTimeout(int socketTimeout) {
    this.socketTimeout = socketTimeout;
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public String getJsonPath() {
    return jsonPath;
  }

  public void setJsonPath(String jsonPath) {
    this.jsonPath = jsonPath;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getPassWard() {
    return passWard;
  }

  public void setPassWard(String passWard) {
    this.passWard = passWard;
  }


  public String getScheme() {
    return scheme;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  public String getHeader() {
    return header;
  }

  public void setHeader(String header) {
    this.header = header;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }


  public String getAisResource() {
    return aisResource;
  }

  public void setAisResource(String aisResource) {
    this.aisResource = aisResource;
  }


}
