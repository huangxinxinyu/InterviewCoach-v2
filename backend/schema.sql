-- -----------------------------------------------------
-- Table `user`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `email` VARCHAR(255) NOT NULL COMMENT '用户邮箱，唯一',
  `password` VARCHAR(255) NOT NULL COMMENT '加密后的密码',
  `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '用户角色（如USER, ADMIN）',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uq_email` (`email` ASC) VISIBLE)
ENGINE = InnoDB
COMMENT = '用户信息表';


-- -----------------------------------------------------
-- Table `question`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  `text` TEXT NOT NULL COMMENT '题目的文本内容',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`))
ENGINE = InnoDB
COMMENT = '题目表';

CREATE TABLE IF NOT EXISTS `answer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '答案ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID，关联question表',
  `text` TEXT NOT NULL COMMENT '答案内容',
  PRIMARY KEY (`id`),
  INDEX `idx_answer_question_id` (`question_id` ASC) VISIBLE,
  CONSTRAINT `fk_answer_question`
    FOREIGN KEY (`question_id`)
    REFERENCES `question` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
COMMENT = '答案表';

-- -----------------------------------------------------
-- Table `tag`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` VARCHAR(50) NOT NULL COMMENT '标签名称，唯一',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uq_tag_name` (`name` ASC) VISIBLE)
ENGINE = InnoDB
COMMENT = '标签表';


-- -----------------------------------------------------
-- Table `question_tag`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `question_tag` (
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `tag_id` BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`question_id`, `tag_id`),
  INDEX `idx_tag_id` (`tag_id` ASC) VISIBLE,
  CONSTRAINT `fk_question_tag_question`
    FOREIGN KEY (`question_id`)
    REFERENCES `question` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_question_tag_tag`
    FOREIGN KEY (`tag_id`)
    REFERENCES `tag` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
COMMENT = '题目与标签关联表';


-- -----------------------------------------------------
-- Table `session`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `mode` ENUM('structured_template', 'structured_set', 'single_topic') NOT NULL COMMENT '会话模式',
  `expected_question_count` INTEGER NULL COMMENT '期望的题目数量',
  `asked_question_count` INTEGER NULL COMMENT 'AI已经提问的题目数量',
  `completed_question_count` INTEGER NULL COMMENT '用户已经回答的题目数量',
  `started_at` DATETIME NOT NULL COMMENT '会话开始时间',
  `ended_at` DATETIME NULL COMMENT '会话结束时间',
  `is_active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '会话是否活跃',
  `question_queue` JSON COMMENT '题目队列(JSON数组存储question_id)',
  `current_question_id` BIGINT NULL COMMENT '当前题目ID',
  `queue_position` INTEGER NOT NULL DEFAULT 0 COMMENT '队列当前位置',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id` ASC) VISIBLE,
  INDEX `idx_session_current_question` (`current_question_id` ASC),
  INDEX `idx_session_user_active` (`user_id`, `is_active`),
  INDEX `idx_session_created_user` (`user_id`, `started_at`),
  CONSTRAINT `fk_session_user`
    FOREIGN KEY (`user_id`)
    REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_session_current_question`
    FOREIGN KEY (`current_question_id`)
    REFERENCES `question` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE)
ENGINE = InnoDB
COMMENT = '会话表';

-- -----------------------------------------------------
-- Table `message`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `type` ENUM('ai', 'user') NOT NULL COMMENT '消息类型',
  `text` TEXT NOT NULL COMMENT '消息内容',
  `created_at` DATETIME NOT NULL COMMENT '消息创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_message_session` (`session_id` ASC) VISIBLE,
  CONSTRAINT `fk_message_session`
    FOREIGN KEY (`session_id`)
    REFERENCES `session` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
COMMENT = '会话消息表';


CREATE TABLE IF NOT EXISTS `user_attempt` (
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `attempt_number` INTEGER NOT NULL DEFAULT 1 COMMENT '尝试次数',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`, `question_id`),
  INDEX `idx_question_id` (`question_id` ASC) VISIBLE,
  CONSTRAINT `fk_user_attempt_user`
    FOREIGN KEY (`user_id`)
    REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_user_attempt_question`
    FOREIGN KEY (`question_id`)
    REFERENCES `question` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
COMMENT = '用户题目尝试次数表';


-- -----------------------------------------------------
-- Table `question_set`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `question_set` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '题集ID',
  `name` VARCHAR(255) NOT NULL COMMENT '题集名称',
  `description` TEXT NULL COMMENT '题集描述',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`))
ENGINE = InnoDB
COMMENT = '题集表';


-- -----------------------------------------------------
-- Table `question_set_item`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `question_set_item` (
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `question_set_id` BIGINT NOT NULL COMMENT '题集ID',
  PRIMARY KEY (`question_id`, `question_set_id`),
  INDEX `idx_qsi_question_set_id` (`question_set_id` ASC) VISIBLE,
  CONSTRAINT `fk_qsi_question`
    FOREIGN KEY (`question_id`)
    REFERENCES `question` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_qsi_question_set`
    FOREIGN KEY (`question_set_id`)
    REFERENCES `question_set` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
COMMENT = '题集与题目关联表';


-- -----------------------------------------------------
-- Table `question_set_collection`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `question_set_collection` (
  `question_set_id` BIGINT NOT NULL COMMENT '题集ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`question_set_id`, `user_id`),
  INDEX `idx_qsc_user_id` (`user_id` ASC) VISIBLE,
  CONSTRAINT `fk_qsc_question_set`
    FOREIGN KEY (`question_set_id`)
    REFERENCES `question_set` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_qsc_user`
    FOREIGN KEY (`user_id`)
    REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
COMMENT = '用户收藏题集表';



-- -----------------------------------------------------
-- Table `template`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `name` VARCHAR(255) NOT NULL COMMENT '模板名称',
  `content` TEXT NULL COMMENT '模板内容',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uq_template_name` (`name` ASC) VISIBLE)
ENGINE = InnoDB
COMMENT = '模板表';

