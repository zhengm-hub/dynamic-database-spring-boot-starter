package com.dynamic.ds.config.strategy;

import javax.sql.DataSource;
import java.util.List;

/**
 * 多数据源选择策略接口
 * 一般默认为负载均衡策略
 *
 * @author zhming
 * @since 1.0.0
 */
public interface DynamicDataSourceStrategy {

    /**
     * 决定当前数据源
     *
     * @param dataSources 数据源选择库
     * @return dataSource 所选择的数据源
     */
    DataSource determineDataSource(List<DataSource> dataSources);
}
