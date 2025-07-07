CREATE TABLE IF NOT EXISTS `local_message_table` (
                                       `id` bigint NOT NULL COMMENT '主键',
                                       `tenant_id` varchar(20)     default '000000'           comment '租户编号',
                                       `class_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类名',
                                       `method_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '方法名',
                                       `method_params` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '方法参数',
                                       `param_types` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '方法类型',
                                       `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
                                       `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'pending' COMMENT '事务执行状态',
                                       `retry_times` int NOT NULL DEFAULT '0' COMMENT '重试次数',
                                       `max_retry_times` int NOT NULL DEFAULT '3' COMMENT '最大重试次数',
                                       `error_message` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误信息',
                                       `created_time` datetime DEFAULT NULL,
                                       `updated_time` datetime DEFAULT NULL,
                                       `executed_time` datetime DEFAULT NULL,
                                       PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='本地事务消息表';
