package com.gxa.pipe.config;

import java.util.Properties;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.pagehelper.PageInterceptor;

@Configuration
public class MybatisPageHelperConfig {
  @Bean
  public Interceptor pageInterceptor() {
    PageInterceptor interceptor = new PageInterceptor();
    Properties properties = new Properties();
    properties.setProperty("helperDialect", "mysql");
    properties.setProperty("reasonable", "true");
    properties.setProperty("supportMethodsArguments", "true");
    properties.setProperty("params", "count=countSql");
    interceptor.setProperties(properties);
    return interceptor;
  }
}