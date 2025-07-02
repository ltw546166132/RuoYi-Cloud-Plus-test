CREATE TABLE IF NOT EXISTS "user" (
                                      id BIGSERIAL PRIMARY KEY,
                                      user_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- PostgreSQL的注释语法
COMMENT ON TABLE "user" IS '用户表';
COMMENT ON COLUMN "user".user_name IS '用户名';
COMMENT ON COLUMN "user".email IS '邮箱';
