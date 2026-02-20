CREATE DATABASE orodisk;

USE orodisk;

DROP TABLE IF EXISTS user;
CREATE TABLE user (
    user_id BIGINT AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    total_quota BIGINT NOT NULL,
    used_quota BIGINT DEFAULT 0,
    status INTEGER DEFAULT 1 NOT NULL,  -- 0:forbidden 1:normal 2:unregister
    type INTEGER DEFAULT 1 NOT NULL,    -- 0:admin 1:user 2:vip
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
);
INSERT INTO user (username, password, total_quota, type) VALUES('admin', '$2a$10$54WUd2ByCnHul.r7ZaXzAeZCMs8XpzPcUngmzJiLXIK2Z.RGSNkaC', 104857600, 0);

DROP TABLE IF EXISTS file;
CREATE TABLE file (
    file_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '所属用户',
    parent_id BIGINT NOT NULL COMMENT '父目录ID（0表示根目录）',
    storage_id BIGINT NOT NULL COMMENT '对应的实际文件id',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_type TINYINT NOT NULL DEFAULT 1 COMMENT '文件类型',
    file_size BIGINT NOT NULL COMMENT '文件尺寸，用于方便查询的冗余',
    status TINYINT DEFAULT 0 COMMENT '0为正常，1为删除，2为回收站',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (file_id),
    UNIQUE KEY idx_idempotent (user_id, parent_id, file_name, status)
);

DROP TABLE IF EXISTS storage;
CREATE TABLE storage (
    storage_id BIGINT NOT NULL AUTO_INCREMENT,
    storage_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小（文件夹为0）',
    storage_path VARCHAR(500) NOT NULL DEFAULT '' COMMENT '实际存储的相对路径',
    md5 varchar(32) NOT NULL COMMENT '文件MD5',
    transcode_status TINYINT DEFAULT 2 COMMENT '0:转码中 1转码失败 2:转码成功',
    ref_count INTEGER COMMENT '引用计数',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (storage_id),
    UNIQUE KEY idx_md5 (md5)
);

-- 分片记录表
DROP TABLE IF EXISTS file_chunk;
CREATE TABLE file_chunk (
    chunk_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_identifier VARCHAR(64) NOT NULL COMMENT '文件唯一标识(MD5)',
    chunk_number INT NOT NULL COMMENT '分片序号(从1开始)',
    chunk_size BIGINT NOT NULL COMMENT '分片大小(字节)',
    total_chunks INT NOT NULL COMMENT '总分片数',
    total_size BIGINT NOT NULL COMMENT '文件总大小',
    storage_path VARCHAR(512) NOT NULL COMMENT '分片存储路径',
    user_id BIGINT NOT NULL,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_identifier (file_identifier),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分片记录表';

-- file_share
DROP TABLE IF EXISTS file_share;
CREATE TABLE file_share (
    share_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    share_code VARCHAR(16) NOT NULL UNIQUE COMMENT '分享码（短链接标识）',
    file_id BIGINT NOT NULL COMMENT '分享的文件ID',
    user_id BIGINT NOT NULL COMMENT '分享者ID',
    password VARCHAR(8) COMMENT '访问密码（可选）',
    expire_time DATETIME COMMENT '过期时间（NULL表示永久）',
    max_downloads INT DEFAULT -1 COMMENT '最大下载次数（-1表示无限制）',
    current_downloads INT DEFAULT 0 COMMENT '当前下载次数',
    allow_download TINYINT DEFAULT 1 COMMENT '是否允许下载',
    status INT DEFAULT 0 COMMENT '0-有效 1-已取消 2-已过期',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_share_code (share_code),
    INDEX idx_user (user_id),
    INDEX idx_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分享表';

DROP TABLE IF EXISTS ai_conversation;
CREATE TABLE ai_conversation (
    conversation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(255) COMMENT '对话标题（可自动生成）',
    context_type TINYINT DEFAULT 0 COMMENT '上下文类型: 0-全局对话 1-文件夹对话 2-文件对话',
    context_id BIGINT COMMENT '关联的文件夹/文件ID（context_type>0时有效）',
    model_name VARCHAR(50) COMMENT '使用的模型名称',
    status TINYINT DEFAULT 0 COMMENT '0-正常 1-已删除',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_context (context_type, context_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话会话表';

DROP TABLE IF EXISTS ai_message;
CREATE TABLE ai_message (
    message_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL COMMENT '对话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色: user/assistant/system',
    content TEXT NOT NULL COMMENT '消息内容',
    token_count INT COMMENT 'token数量（用于统计）',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI消息记录表';

