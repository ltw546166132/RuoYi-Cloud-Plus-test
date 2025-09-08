package org.dromara.workflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 按钮权限枚举
 *
 * @author AprilWind
 */
@Getter
@AllArgsConstructor
public enum RolePermissionEnum implements NodeExtEnum {

    /**
     * 是否弹窗选人
     */
    MATCH_ROLE_POSITION_LEVEL("是否匹配", "match_role_position_level", false),
    ;


    private final String label;
    private final String value;
    private final boolean selected;

}

