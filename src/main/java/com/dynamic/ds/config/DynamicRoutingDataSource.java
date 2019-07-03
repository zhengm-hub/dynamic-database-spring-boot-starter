package com.dynamic.ds.config;

import com.dynamic.ds.config.provider.DynamicDataSourceProvider;
import com.dynamic.ds.config.strategy.DynamicDataSourceStrategy;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 核心动态数据源组件
 *
 * @author TaoYu Kanyuxia
 * @since 1.0.0
 */
@Slf4j
public class DynamicRoutingDataSource extends AbstractRoutingDataSource implements InitializingBean, DisposableBean {

    @Setter
    private DynamicDataSourceProvider provider;
    @Setter
    private Class<? extends DynamicDataSourceStrategy> strategy;
    @Setter
    private String primary;
    /**
     * 所有数据库
     */
    private Map<String, DataSource> dataSourceMap = new LinkedHashMap<>();

    @Override
    protected DataSource determineCurrentLookupKey() {
        return getDataSource(DynamicDataSourceContextHolder.peek());
    }

    private DataSource determinePrimaryDataSource() {
        log.info("从默认数据源中返回数据");
        return dataSourceMap.get(primary);
    }

    /**
     * 获取当前所有的数据源
     *
     * @return 当前所有数据源
     */
    public Map<String, DataSource> getCurrentDataSources() {
        return dataSourceMap;
    }

    /**
     * 获取数据源
     *
     * @param ds 数据源名称
     * @return 数据源
     */
    public DataSource getDataSource(String ds) {
        if (StringUtils.isEmpty(ds)) {
            return determinePrimaryDataSource();
        } else if (dataSourceMap.containsKey(ds)) {
            log.info("从 {} 单数据源中返回数据源", ds);
            return dataSourceMap.get(ds);
        }
        return determinePrimaryDataSource();
    }

    /**
     * 添加数据源
     *
     * @param ds         数据源名称
     * @param dataSource 数据源
     */
    public synchronized void addDataSource(String ds, DataSource dataSource) {
        dataSourceMap.put(ds, dataSource);
        log.info("动态数据源-加载 {} 成功", ds);
    }

    /**
     * 删除数据源
     *
     * @param ds 数据源名称
     */
    public synchronized void removeDataSource(String ds) {
        if (dataSourceMap.containsKey(ds)) {
            DataSource dataSource = dataSourceMap.get(ds);
            dataSourceMap.remove(ds);
            log.info("动态数据源-删除 {} 成功", ds);
        } else {
            log.warn("动态数据源-未找到 {} 数据源");
        }
    }

    @Override
    public void destroy() throws Exception {
        log.info("closing dynamicDatasource  ing....");
        for (Map.Entry<String, DataSource> item : dataSourceMap.entrySet()) {
            DataSource dataSource = item.getValue();
            Class<? extends DataSource> clazz = dataSource.getClass();
            try {
                Method closeMethod = clazz.getDeclaredMethod("close");
                closeMethod.invoke(dataSource);
            } catch (NoSuchMethodException e) {
                log.warn("关闭数据源 {} 失败,", item.getKey());
            }
        }
    }

    @Override
    public void afterPropertiesSet() {
        Map<String, DataSource> dataSources = provider.loadDataSources();
        log.info("初始共加载 {} 个数据源", dataSources.size());
        //添加并分组数据源
        for (Map.Entry<String, DataSource> dsItem : dataSources.entrySet()) {
            addDataSource(dsItem.getKey(), dsItem.getValue());
        }
        if (dataSourceMap.containsKey(primary)) {
            log.info("当前的默认数据源是单数据源，数据源名为 {}", primary);
        } else {
            throw new RuntimeException("请检查primary默认数据库设置");
        }
    }

    @Override
    public DataSource determineTargetDataSource() {
        DataSource dataSource = this.determineCurrentLookupKey();
        if (dataSource == null) {
            throw new IllegalStateException("Cannot determine target DataSource");
        } else {
            return dataSource;
        }
    }
}