package org.dromara.common.localmessagetable.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("local_message_table")
public class LocalMessageTransactionEntity {
    @TableId
    private Long id;

    private String className;

    private String methodName;

    private String methodParams;

    private String paramTypes;

    private String description;

    private String status;

    private Integer retryTimes = 0;

    private Integer maxRetryTimes = 3;

    private String errorMessage;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private LocalDateTime executedTime;

    private String tenantId;
}
