package org.dromara.common.localmessagetable.config;

import org.dromara.common.core.factory.YmlPropertySourceFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableScheduling
@PropertySource(value = "classpath:common-localmessagetable.yml", factory = YmlPropertySourceFactory.class)
public class LocalMessageTableConfig {
}
