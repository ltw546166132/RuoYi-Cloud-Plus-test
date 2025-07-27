package org.dromara.common.localmessagetable.aspectj;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.dromara.common.core.utils.ObjectUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.localmessagetable.annotation.LocalMessageTransaction;
import org.dromara.common.localmessagetable.domain.LocalMessageTransactionEntity;
import org.dromara.common.localmessagetable.events.DeserializeEvent;
import org.dromara.common.localmessagetable.service.ILocalMessageTransactionService;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.lang.reflect.Method;

@Aspect
@AutoConfiguration
public class LocalMessageTransactionAspect {
    @Resource
    private ILocalMessageTransactionService localMessageTransactionService;

    private final ObjectMapper objectMapper = JsonUtils.getObjectMapper();
    private static final ThreadLocal<String> SERIALIZED_METHOD = new ThreadLocal<>();

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
        if(StringUtils.isNotBlank(SERIALIZED_METHOD.get()) && StringUtils.equals(className+":"+methodName, SERIALIZED_METHOD.get())){
            return joinPoint.proceed();
        }
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
        if(ObjectUtils.isNotNull(LoginHelper.getUserId())){
            localMessageTransactionEntity.setUserId(LoginHelper.getUserId());
        }
        // 保存到数据库（在当前事务中）
        localMessageTransactionService.saveMessage(localMessageTransactionEntity);

        // 注册事务同步回调
        SpringUtils.publishEvent(new DeserializeEvent(this, localMessageTransactionEntity.getId(), StpUtil.isLogin()? StpUtil.getTokenValue() : null));
        // 不执行原方法，直接返回
        return null;
    }

    public static void markMethod(boolean executing, String className, String methodName) {
        if (executing) {
            SERIALIZED_METHOD.set(className+":"+methodName);
        } else {
            SERIALIZED_METHOD.remove();
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
