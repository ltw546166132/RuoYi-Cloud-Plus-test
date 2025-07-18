package org.dromara.common.localmessagetable.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.ObjectUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.localmessagetable.aspectj.LocalMessageTransactionAspect;
import org.dromara.common.localmessagetable.domain.LocalMessageTransactionEntity;
import org.dromara.common.localmessagetable.enums.LocalMessageStatus;
import org.dromara.common.localmessagetable.mapper.LocalMessageTransactionEntityMapper;
import org.dromara.common.localmessagetable.service.ILocalMessageTransactionService;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class LocalMessageTransactionServiceImpl implements ILocalMessageTransactionService {
    @Resource
    private LocalMessageTransactionEntityMapper baseMapper;

    private final ObjectMapper objectMapper = JsonUtils.getObjectMapper();

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void saveMessage(LocalMessageTransactionEntity message) {
        message.setRetryTimes(0);
        message.setStatus(LocalMessageStatus.PENDING.getCode());
        baseMapper.insert(message);
        log.info("保存本地消息事务: {}.{}", message.getClassName(), message.getMethodName());
    }

    /**
     * 异步执行消息
     */
    @Async
    public void executeMessageAsync(Long messageId) {
        try {
            executeMessage(messageId);
        } catch (Exception e) {
            log.error("异步执行消息失败: {}", messageId, e);
        }
    }

    /**
     * 执行单个消息
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeMessage(Long messageId) {
        boolean retryLock = RedisUtils.setObjectIfAbsent("local_message_transaction_lock:" + messageId, "", Duration.ofMillis(30000));
        if (!retryLock) {
            log.warn("消息处理中，跳过: {}", messageId);
            return;
        }
        LocalMessageTransactionEntity message = baseMapper.selectById(messageId);
        if (ObjectUtils.isNull(message)) {
            log.warn("消息不存在: {}", messageId);
            return;
        }

        if (StringUtils.equals(message.getStatus(), LocalMessageStatus.SUCCESS.getCode())) {
            log.info("消息已执行成功，跳过: {}", messageId);
            return;
        }

        if (message.getRetryTimes() >= message.getMaxRetryTimes()) {
            log.warn("消息超过最大重试次数，跳过: {}", messageId);
            return;
        }

        try {
            // 执行业务方法
            executeBusinessMethod(message);

            // 更新状态为成功
            message.setStatus(LocalMessageStatus.SUCCESS.getCode());
            message.setExecutedTime(LocalDateTime.now());
            baseMapper.updateById(message);

            log.info("消息执行成功: {}.{}", message.getClassName(), message.getMethodName());

        } catch (Exception e) {
            // 增加重试次数
            message.setRetryTimes(message.getRetryTimes() + 1);
            message.setErrorMessage(e.getMessage());

            if (message.getRetryTimes() >= message.getMaxRetryTimes()) {
                message.setStatus(LocalMessageStatus.MAX_RETRY_EXCEEDED.getCode());
                log.error("消息执行失败，超过最大重试次数: {}.{}",
                    message.getClassName(), message.getMethodName(), e);
            } else {
                message.setStatus(LocalMessageStatus.FAILED.getCode());
                log.warn("消息执行失败，重试次数: {}/{}, 错误: {}",
                    message.getRetryTimes(), message.getMaxRetryTimes(), e.getMessage());
            }

            baseMapper.updateById(message);
        }
    }
    /**
     * 根据参数类型正确反序列化参数值
     */
    private Object[] deserializeParams(String methodParams, Class<?>[] paramTypes) throws Exception {
        if (paramTypes.length == 0) {
            return new Object[0];
        }

        // 先反序列化为JsonNode数组，保持原始JSON结构
        com.fasterxml.jackson.databind.JsonNode[] jsonNodes = objectMapper.readValue(methodParams, com.fasterxml.jackson.databind.JsonNode[].class);

        Object[] params = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            if (jsonNodes[i] == null || jsonNodes[i].isNull()) {
                params[i] = null;
            } else {
                // 根据目标类型进行转换
                params[i] = objectMapper.treeToValue(jsonNodes[i], paramTypes[i]);
            }
        }

        return params;
    }


    /**
     * 执行业务方法
     */
    private void executeBusinessMethod(LocalMessageTransactionEntity message) throws Exception {
        // 获取目标类
        Class<?> targetClass = Class.forName(message.getClassName());
        Object targetBean = SpringUtils.getBean(targetClass);

        // 反序列化参数类型
        String[] paramTypeNames = objectMapper.readValue(message.getParamTypes(), String[].class);
        Class<?>[] paramTypes = new Class[paramTypeNames.length];
        for (int i = 0; i < paramTypeNames.length; i++) {
            paramTypes[i] = Class.forName(paramTypeNames[i]);
        }

        // 反序列化参数值
        Object[] params = deserializeParams(message.getMethodParams(), paramTypes);
        // 获取方法并执行
        Method method = targetClass.getMethod(message.getMethodName(), paramTypes);
        // 设置标记，表示正在执行本地消息事务
        try{
            LocalMessageTransactionAspect.setExecutingLocalMessage(true);
            if(StringUtils.isNotBlank(message.getTenantId())){
                TenantHelper.dynamic(message.getTenantId(),() -> {
                    try {
                        return method.invoke(targetBean, params);
                    } catch (Exception e) {
                        throw new ServiceException(e.getMessage());
                    }
                });
            }else{
                method.invoke(targetBean, params);
            }
        } finally {
            LocalMessageTransactionAspect.setExecutingLocalMessage(false);
        }
    }

    /**
     * 定时任务：处理待执行的消息
     */
    @Override
    public void processPendingMessages() {
        List<LocalMessageTransactionEntity> pendingMessages = baseMapper.selectList(Wrappers.<LocalMessageTransactionEntity>lambdaQuery().in(LocalMessageTransactionEntity::getStatus, LocalMessageStatus.PENDING.getCode(), LocalMessageStatus.FAILED.getCode()).orderByDesc(LocalMessageTransactionEntity::getCreateTime).apply("retry_times < max_retry_times"));
        log.info("发现待处理本地事务消息数量: {}", pendingMessages.size());

        for (LocalMessageTransactionEntity message : pendingMessages) {
            try {
                executeMessage(message.getId());
            } catch (Exception e) {
                log.error("定时任务执行消息失败: {}", message.getId(), e);
            }
        }
    }

    /**
     * 定时任务：删除过期的消息 status等于success的数据只保留最近7天
     */
    @Override
    public void deleteExpiredMessages(){
        baseMapper.delete(Wrappers.<LocalMessageTransactionEntity>lambdaQuery().eq(LocalMessageTransactionEntity::getStatus, LocalMessageStatus.SUCCESS.getCode()).lt(LocalMessageTransactionEntity::getCreateTime, LocalDateTime.now().minusDays(7)));
    }
}
