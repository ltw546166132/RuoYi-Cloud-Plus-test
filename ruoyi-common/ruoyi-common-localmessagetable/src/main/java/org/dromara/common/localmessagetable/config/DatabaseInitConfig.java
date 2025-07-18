package org.dromara.common.localmessagetable.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.mybatis.helper.DataBaseHelper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@AutoConfiguration
public class DatabaseInitConfig {
    private static final DynamicRoutingDataSource DS = SpringUtils.getBean(DynamicRoutingDataSource.class);
    @Bean
    public DataSourceInitializer dataSourceInitializer() {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(DS.determineDataSource());
        // 根据数据库类型选择SQL文件
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        if(DataBaseHelper.isMySql()){
            populator.addScript(new ClassPathResource("schema-mysql.sql"));
        }else if(DataBaseHelper.isPostgerSql()){
            populator.addScript(new ClassPathResource("schema-postgresql.sql"));
        }else {
            populator.addScript(new ClassPathResource("schema.sql"));
        }
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}
