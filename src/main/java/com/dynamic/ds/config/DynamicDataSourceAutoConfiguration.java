package com.dynamic.ds.config;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.dynamic.ds.aop.DynamicDataSourceAnnotationAdvisor;
import com.dynamic.ds.aop.DynamicDataSourceAnnotationInterceptor;
import com.dynamic.ds.config.property.druid.DruidDynamicDataSourceConfiguration;
import com.dynamic.ds.config.provider.DynamicDataSourceProvider;
import com.dynamic.ds.config.provider.YmlDynamicDataSourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.Advice;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;

/**
 * 一、原理区别：
 *
 * java动态代理是利用反射机制生成一个实现代理接口的匿名类，在调用具体方法前调用InvokeHandler来处理。
 *
 * 而cglib动态代理是利用asm开源包，对代理对象类的class文件加载进来，通过修改其字节码生成子类来处理。
 *
 * 1、如果目标对象实现了接口，默认情况下会采用JDK的动态代理实现AOP
 * 2、如果目标对象实现了接口，可以强制使用CGLIB实现AOP
 *
 * 3、如果目标对象没有实现了接口，必须采用CGLIB库，spring会自动在JDK动态代理和CGLIB之间转换
 *
 * 如何强制使用CGLIB实现AOP？
 *  （1）添加CGLIB库，SPRING_HOME/cglib/*.jar
 *  （2）在spring配置文件中加入<aop:aspectj-autoproxy proxy-target-class="true"/>
 *
 * JDK动态代理和CGLIB字节码生成的区别？
 *  （1）JDK动态代理只能对实现了接口的类生成代理，而不能针对类
 *  （2）CGLIB是针对类实现代理，主要是对指定的类生成一个子类，覆盖其中的方法
 *    因为是继承，所以该类或方法最好不要声明成final
 *
 * 动态数据源核心自动配置类
 * proxyTargetClass = true 是否使用cglib方式生成代理,默认使用jdk代理方式
 * exposeProxy = true 是否提供导出给目标类获取代理对象：AopContext.currentProxy()
 *
 * @author zhming
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DynamicDataSourceProperties.class)
@ConditionalOnClass({EnableAspectJAutoProxy.class, Aspect.class, Advice.class})
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@Import(DruidDynamicDataSourceConfiguration.class)
@EnableAspectJAutoProxy(exposeProxy = true, proxyTargetClass = true)
public class DynamicDataSourceAutoConfiguration {

    @Autowired
    private DynamicDataSourceProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public DynamicDataSourceCreator dynamicDataSourceCreator() {
        DynamicDataSourceCreator dynamicDataSourceCreator = new DynamicDataSourceCreator();
        dynamicDataSourceCreator.setDruidGlobalConfig(properties.getDruid());
        dynamicDataSourceCreator.setHikariGlobalConfig(properties.getHikari());
        return dynamicDataSourceCreator;
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicDataSourceProvider dynamicDataSourceProvider(DynamicDataSourceCreator dynamicDataSourceCreator) {
        return new YmlDynamicDataSourceProvider(properties, dynamicDataSourceCreator);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DynamicDataSourceProvider dynamicDataSourceProvider) {
        DynamicRoutingDataSource dataSource = new DynamicRoutingDataSource();
        dataSource.setPrimary(properties.getPrimary());
        dataSource.setStrategy(properties.getStrategy());
        dataSource.setProvider(dynamicDataSourceProvider);
        return dataSource;
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicDataSourceAnnotationAdvisor dynamicDatasourceAnnotationAdvisor() {
        DynamicDataSourceAnnotationInterceptor interceptor = new DynamicDataSourceAnnotationInterceptor();
        DynamicDataSourceAnnotationAdvisor advisor = new DynamicDataSourceAnnotationAdvisor(interceptor);
        advisor.setOrder(properties.getOrder());
        return advisor;
    }

    @Bean
    public PaginationInterceptor paginationInterceptor() {
        return new PaginationInterceptor();
    }
}