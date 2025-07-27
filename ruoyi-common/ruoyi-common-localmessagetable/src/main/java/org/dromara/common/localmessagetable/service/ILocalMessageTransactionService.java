package org.dromara.common.localmessagetable.service;

import org.dromara.common.localmessagetable.domain.LocalMessageTransactionEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface ILocalMessageTransactionService {
    void saveMessage(LocalMessageTransactionEntity message);

    void executeMessageAsync(Long id, String tokenValue);

    void processPendingMessages();

    void deleteExpiredMessages();
}
