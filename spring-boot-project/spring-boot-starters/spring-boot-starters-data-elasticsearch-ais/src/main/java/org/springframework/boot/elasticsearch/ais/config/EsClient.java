package org.springframework.boot.elasticsearch.ais.config;

import org.springframework.boot.elasticsearch.ais.utils.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.stereotype.Component;

/**
 * . get RestClient
 *
 * @author lihang
 * @email 631533483@qq.com
 */

@Component
public class EsClient {

  private Config config;

  public EsClient(Config config) {
    this.config = config;
  }

  /**
   * get restClient.
   *
   * @return RestClient
   */
  public RestClient getRestClient() {
    String[] url = config.getUrl().split(",");
    String scheme = config.getScheme().toString().trim();
    HttpHost[] httpHostList = new HttpHost[url.length];
    for (int i = 0; i < url.length; i++) {
      String u = url[i].trim();
      String hostName = u.substring(0, u.indexOf(":")).trim();
      String port = u.substring(u.indexOf(":") + 1, u.length()).trim();
      HttpHost httpHost = new HttpHost(hostName, Integer.parseInt(port), scheme);
      httpHostList[i] = httpHost;
    }
    RestClientBuilder restClientBuilder = RestClient.builder(httpHostList);
    String userName = config.getUserName();
    String passWard = config.getPassWard();
    //设置登录验证
    if (StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(passWard)) {
      CredentialsProvider credentialsProvider =
          new BasicCredentialsProvider();
      credentialsProvider.setCredentials(AuthScope.ANY,
          new UsernamePasswordCredentials(userName, passWard));
      restClientBuilder
          .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(
                HttpAsyncClientBuilder httpAsyncClientBuilder) {
              //httpAsyncClientBuilder.disableAuthCaching();
              httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
              httpAsyncClientBuilder
                  .setDefaultIOReactorConfig(IOReactorConfig.custom().setIoThreadCount(1).build());
              return httpAsyncClientBuilder;
            }
          });
    }

    //设置请求头
    if (StringUtils.isNotBlank(config.getHeader()) && StringUtils.isNotBlank(config.getValue())) {
      Header[] defaultHeaders = new Header[]{
          new BasicHeader(config.getHeader(), config.getValue())};
      restClientBuilder.setDefaultHeaders(defaultHeaders);
    }

    int socketTimeout = config.getSocketTimeout();
    int connectTimeout = config.getConnectTimeout();

    restClientBuilder.setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
      @Override
      public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder builder) {
        builder.setSocketTimeout(socketTimeout);
        builder.setConnectTimeout(connectTimeout);
        return builder;
      }
    });

    return restClientBuilder.build();
  }


}
