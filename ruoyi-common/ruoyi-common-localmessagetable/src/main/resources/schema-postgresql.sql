CREATE TABLE IF NOT EXISTS local_message_table (
                                     id int8 NOT NULL, -- 主键
                                     class_name varchar(256) NOT NULL, -- 类名
                                     method_name varchar(256) NOT NULL, -- 方法名
                                     method_params varchar(256) NULL, -- 方法参数
                                     param_types varchar(256) NULL, -- 方法类型
                                     description varchar(512) NULL, -- 描述
                                     status varchar(32) NOT NULL DEFAULT 'pending'::character varying, -- 事务执行状态
                                     retry_times int4 NOT NULL DEFAULT 0, -- 重试次数
                                     max_retry_times int4 NOT NULL DEFAULT 3, -- 最大重试次数
                                     error_message varchar(512) NULL, -- 错误信息
                                     created_time timestamp NULL,
                                     updated_time timestamp NULL,
                                     executed_time timestamp NULL,
                                     CONSTRAINT local_message_table_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE public.local_message_table IS '本地事务消息表';

-- Column comments

COMMENT ON COLUMN public.local_message_table.id IS '主键';
COMMENT ON COLUMN public.local_message_table.class_name IS '类名';
COMMENT ON COLUMN public.local_message_table.method_name IS '方法名';
COMMENT ON COLUMN public.local_message_table.method_params IS '方法参数';
COMMENT ON COLUMN public.local_message_table.param_types IS '方法类型';
COMMENT ON COLUMN public.local_message_table.description IS '描述';
COMMENT ON COLUMN public.local_message_table.status IS '事务执行状态';
COMMENT ON COLUMN public.local_message_table.retry_times IS '重试次数';
COMMENT ON COLUMN public.local_message_table.max_retry_times IS '最大重试次数';
COMMENT ON COLUMN public.local_message_table.error_message IS '错误信息';
