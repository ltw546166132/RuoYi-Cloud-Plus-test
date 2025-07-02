package org.dromara.common.localmessagetable.domain.dto;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.localmessagetable.domain.LocalMessageTransactionEntity;
import java.time.LocalDateTime;
@Data
@AutoMapper(target = LocalMessageTransactionEntity.class, reverseConvertGenerate = false)
public class LocalMessageTransactionDto {
    private Long id;

    private String className;

    private String methodName;

    private String methodParams;

    private String paramTypes;

    private String description;

    private Integer retryTimes = 0;

    private Integer maxRetryTimes = 3;

    private String errorMessage;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private LocalDateTime executedTime;
}
