package org.dromara.common.localmessagetable.service.impl;

import jakarta.annotation.Resource;
import org.dromara.common.localmessagetable.mapper.LocalMessageTransactionEntityMapper;
import org.dromara.common.localmessagetable.service.ILocalMessageTransactionService;
import org.springframework.stereotype.Service;

@Service
public class LocalMessageTransactionServiceImpl implements ILocalMessageTransactionService {
    @Resource
    private LocalMessageTransactionEntityMapper baseMapper;
}
