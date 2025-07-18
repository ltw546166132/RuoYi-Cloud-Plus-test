package org.dromara.common.localmessagetable.scheduler;

import jakarta.annotation.Resource;
import org.dromara.common.localmessagetable.service.ILocalMessageTransactionService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LocalMessageTransactionScheduler {
    @Resource
    private ILocalMessageTransactionService localMessageTransactionService;

    /**
     * 每30秒执行一次，处理待执行的本地消息事务
     */
    @Scheduled(fixedRate = 30000)
    public void processMessages() {
        localMessageTransactionService.processPendingMessages();
    }

    /**
     * 每天0点执行一次，删除过期的本地消息事务
     */
    @Async
    @Scheduled(cron = "0 0 0 * * ?")
    public void deleteExpiredMessages() {
        localMessageTransactionService.deleteExpiredMessages();
    }
}
