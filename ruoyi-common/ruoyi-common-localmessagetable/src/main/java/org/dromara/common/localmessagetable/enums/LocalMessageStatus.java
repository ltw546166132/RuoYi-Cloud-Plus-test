package org.dromara.common.localmessagetable.enums;

import lombok.Getter;

@Getter
public enum LocalMessageStatus {
    PENDING("pending", "待执行"),
    SUCCESS("success", "成功"),
    FAIL("fail", "失败"),
    MAX_RETRY_EXCEEDED("max_retry_exceeded", "最大重试次数 exceeded");

    LocalMessageStatus(String code, String message) {
        this.code = code;
        this.message = message;
    }
    private String code;
    private String message;
    public static LocalMessageStatus getEnumByCode(String code) {
        for (LocalMessageStatus value : LocalMessageStatus.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
