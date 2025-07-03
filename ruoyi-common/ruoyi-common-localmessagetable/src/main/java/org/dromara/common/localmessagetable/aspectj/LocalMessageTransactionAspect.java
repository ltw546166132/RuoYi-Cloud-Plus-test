package org.dromara.common.localmessagetable.aspectj;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.localmessagetable.annotation.LocalMessageTransaction;
import org.dromara.common.localmessagetable.domain.LocalMessageTransactionEntity;
import org.dromara.common.localmessagetable.enums.LocalMessageStatus;
import org.dromara.common.localmessagetable.service.ILocalMessageTransactionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.lang.reflect.Method;

@Aspect
@Component
public class LocalMessageTransactionAspect {
    @Resource
    private ILocalMessageTransactionService localMessageTransactionService;

    private final ObjectMapper objectMapper = JsonUtils.getObjectMapper();

    @Around("@annotation(localMessageTransaction)")
    public Object around(ProceedingJoinPoint joinPoint, LocalMessageTransaction localMessageTransaction) throws Throwable {

        // 检查是否在事务中
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // 不在事务中，直接执行原方法
            return joinPoint.proceed();
        }

        // 检查方法返回值类型
        Method method = getMethod(joinPoint);
        if (!method.getReturnType().equals(Void.TYPE)) {
            // 有返回值，直接执行原方法
            return joinPoint.proceed();
        }

        // 准备消息数据
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // 序列化参数
        String methodParams = objectMapper.writeValueAsString(args);
        String paramTypes = getParameterTypes(method);

        // 创建消息记录
        LocalMessageTransactionEntity localMessageTransactionEntity = new LocalMessageTransactionEntity();
        localMessageTransactionEntity.setClassName(className);
        localMessageTransactionEntity.setMethodName(methodName);
        localMessageTransactionEntity.setMethodParams(methodParams);
        localMessageTransactionEntity.setParamTypes(paramTypes);
        localMessageTransactionEntity.setDescription(localMessageTransaction.description());
        localMessageTransactionEntity.setMaxRetryTimes(localMessageTransaction.maxRetryTimes());
        // 保存到数据库（在当前事务中）
        localMessageTransactionService.saveMessage(localMessageTransactionEntity);

        // 注册事务同步回调
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 事务提交成功后，异步执行消息
                localMessageTransactionService.executeMessageAsync(localMessageTransactionEntity.getId());
            }
        });

        // 不执行原方法，直接返回
        return null;
    }

    private Method getMethod(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        Class<?>[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
        }
        return joinPoint.getTarget().getClass().getMethod(methodName, paramTypes);
    }

    private String getParameterTypes(Method method) throws Exception {
        Class<?>[] paramTypes = method.getParameterTypes();
        String[] typeNames = new String[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            typeNames[i] = paramTypes[i].getName();
        }
        return objectMapper.writeValueAsString(typeNames);
    }
}
