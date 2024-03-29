package com.dynamic.ds.config.strategy;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负载均衡策略
 *
 * @author TaoYu Kanyuxia
 * @since 1.0.0
 */
public class LoadBalanceDynamicDataSourceStrategy implements DynamicDataSourceStrategy {

    /**
     * 负载均衡计数器
     */
    private AtomicInteger index = new AtomicInteger(0);

    @Override
    public DataSource determineDataSource(List<DataSource> dataSources) {
        return dataSources.get(Math.abs(index.getAndAdd(1) % dataSources.size()));
    }
}
