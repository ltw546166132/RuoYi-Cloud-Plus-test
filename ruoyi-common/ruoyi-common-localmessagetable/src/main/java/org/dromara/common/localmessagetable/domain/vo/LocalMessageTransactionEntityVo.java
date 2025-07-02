package org.dromara.common.localmessagetable.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.localmessagetable.domain.LocalMessageTransactionEntity;
@Data
@AutoMapper(target = LocalMessageTransactionEntity.class)
public class LocalMessageTransactionEntityVo extends LocalMessageTransactionEntity {
}
