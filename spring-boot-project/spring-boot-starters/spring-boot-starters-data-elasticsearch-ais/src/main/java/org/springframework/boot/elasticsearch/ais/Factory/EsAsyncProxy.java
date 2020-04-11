package org.springframework.boot.elasticsearch.ais.Factory;


import com.alibaba.fastjson.JSONObject;
import org.springframework.boot.elasticsearch.ais.config.Config;
import org.springframework.boot.elasticsearch.ais.config.EsClient;
import org.springframework.boot.elasticsearch.ais.restClient.RestPerform;
import org.springframework.boot.elasticsearch.ais.restClient.RestPerformIpml;
import org.springframework.boot.elasticsearch.ais.utils.JsonUtils;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.stereotype.Component;


/**
 * Dynamic proxy with ElasticsearchAsync annotation interface(Spring， under development).
 *
 * @author lihang
 * @email 631533483@qq.com
 */

@Component
public class EsAsyncProxy implements InvocationHandler {

  private Class<?> interfaceClass;
  private Logger log = LoggerFactory.getLogger(EsAsyncProxy.class);

  public Object bind(Class<?> cls) {
    this.interfaceClass = cls;
    return Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{interfaceClass}, this);
  }

  @Override
  public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
    RestPerform restPerform = new RestPerformIpml(new EsClient(Config.getConfig()));
    String name = method.getDeclaringClass().getName();
    String methodName = method.getName();
    String[] arr = name.split("\\.");
    String jsonPath = arr[arr.length - 1];
    Config config = Config.getConfig();
    JSONObject jsonObject = JsonUtils
        .readJsonFromClassPath(config.getJsonPath() + "/" + jsonPath + ".json");
    String index = jsonObject.getJSONObject(methodName).get("index").toString();
    String script = jsonObject.getJSONObject(methodName).get("script").toString();
    String requestMethod = jsonObject.getJSONObject(methodName).get("requestMethod").toString();
    Type[] types = method.getGenericParameterTypes();
    DefaultParameterNameDiscoverer discover = new DefaultParameterNameDiscoverer();
    String[] typenames = discover.getParameterNames(method);
    for (int i = 0; i < objects.length; i++) {
      script = script.replaceAll("#\\{" + typenames[i] + "\\}", objects[i].toString());
    }
    try {
      Class<?>[] classTypes = method.getParameterTypes();
      String regex = "#\\{[^}]*\\}";
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(script);
      if (classTypes.length > 0 && matcher.find()) {
        script = JsonUtils.replaceEntity(script, classTypes[0], objects[0]);
      }
    } catch (NoSuchFieldException e) {
      log.error(e.toString());
    } catch (NullPointerException e) {
      log.error("There are corresponding fields not filled in 有对应的字段为被填写");
    }

    String returnClass;
    try {
      ParameterizedType type = (ParameterizedType) method.getGenericReturnType();
      Type[] typesReturn = type.getActualTypeArguments();
      if (typesReturn.length > 0) {
        returnClass = typesReturn[0].getTypeName();
        return restPerform
            .executeAsync(requestMethod, index, script, 1, Class.forName(returnClass));
      }
    } catch (ClassCastException e) {
      Type type = method.getReturnType();
      returnClass = type.getTypeName();
      if (returnClass.equals(String.class.getTypeName())) {
        return restPerform.executeJSONAsync(requestMethod, index, script, 1).toString();
      } else {
        return restPerform.executeJSONAsync(requestMethod, index, script, 1);
      }
    }
    return restPerform.executeJSON(requestMethod, index, script);

  }

}