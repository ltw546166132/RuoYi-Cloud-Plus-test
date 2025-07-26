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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.lang.reflect.Method;

@Aspect
@AutoConfiguration
public class LocalMessageTransactionAspect {
    @Resource
    private ILocalMessageTransactionService localMessageTransactionService;

    private final ObjectMapper objectMapper = JsonUtils.getObjectMapper();
    private static final ThreadLocal<Boolean> EXECUTING_LOCAL_MESSAGE = new ThreadLocal<>();


    @Around("@annotation(localMessageTransaction)")
    public Object around(ProceedingJoinPoint joinPoint, LocalMessageTransaction localMessageTransaction) throws Throwable {
// 检查是否正在执行本地消息事务中的方法，避免递归
        if (Boolean.TRUE.equals(EXECUTING_LOCAL_MESSAGE.get())) {
            // 正在执行本地消息事务，直接执行原方法
            return joinPoint.proceed();
        }
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
        // 检查是否在嵌套事务中（当前方法本身开启了新事务）
        String currentTransactionName = TransactionSynchronizationManager.getCurrentTransactionName();
        if (currentTransactionName != null && currentTransactionName.contains(method.getName())) {
            // 当前方法本身开启了新事务，直接执行原方法
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
                if(localMessageTransaction.async()){
                    // 事务提交成功后，异步执行消息
                    localMessageTransactionService.executeMessageAsync(localMessageTransactionEntity.getId());
                }else {
                    localMessageTransactionService.executeMessageSync(localMessageTransactionEntity.getId());
                }
            }
        });

        // 不执行原方法，直接返回
        return null;
    }

    /**
     * 标记正在执行本地消息事务
     */
    public static void setExecutingLocalMessage(boolean executing) {
        if (executing) {
            EXECUTING_LOCAL_MESSAGE.set(true);
        } else {
            EXECUTING_LOCAL_MESSAGE.remove();
        }
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
