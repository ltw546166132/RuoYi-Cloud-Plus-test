package org.dromara.common.localmessagetable.config;

import jakarta.annotation.PostConstruct;
import org.dromara.common.core.factory.YmlPropertySourceFactory;
import org.dromara.common.core.utils.ObjectUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.localmessagetable.aspectj.LocalMessageTransactionAspect;
import org.dromara.common.localmessagetable.mapper.LocalMessageTransactionEntityMapper;
import org.dromara.common.localmessagetable.properties.TenantProperties;
import org.dromara.common.localmessagetable.scheduler.LocalMessageTransactionScheduler;
import org.dromara.common.localmessagetable.service.impl.LocalMessageTransactionServiceImpl;
import org.dromara.common.redis.config.RedisConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(TenantProperties.class)
public class LocalMessageTableConfig {
}
