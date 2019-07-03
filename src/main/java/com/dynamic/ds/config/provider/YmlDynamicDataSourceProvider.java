package com.dynamic.ds.config.provider;

import com.dynamic.ds.config.DynamicDataSourceCreator;
import com.dynamic.ds.config.DynamicDataSourceProperties;
import com.dynamic.ds.config.property.DataSourceProperty;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * YML数据源提供者
 *
 * @author TaoYu Kanyuxia
 * @since 1.0.0
 */
public class YmlDynamicDataSourceProvider implements DynamicDataSourceProvider {

    /**
     * 多数据源参数
     */
    private DynamicDataSourceProperties properties;
    /**
     * 多数据源创建器
     */
    private DynamicDataSourceCreator dynamicDataSourceCreator;

    public YmlDynamicDataSourceProvider(DynamicDataSourceProperties properties, DynamicDataSourceCreator dynamicDataSourceCreator) {
        this.properties = properties;
        this.dynamicDataSourceCreator = dynamicDataSourceCreator;
    }

    @Override
    public Map<String, DataSource> loadDataSources() {
        Map<String, DataSourceProperty> dataSourcePropertiesMap = properties.getDatasource();
        Map<String, DataSource> dataSourceMap = new HashMap<>(dataSourcePropertiesMap.size());
        for (Map.Entry<String, DataSourceProperty> item : dataSourcePropertiesMap.entrySet()) {
            String pollName = item.getKey();
            DataSourceProperty dataSourceProperty = item.getValue();
            dataSourceProperty.setPollName(pollName);
            dataSourceMap.put(pollName, dynamicDataSourceCreator.createDataSource(dataSourceProperty));
        }
        return dataSourceMap;
    }
}
