/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.example.manus.config.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "system_config")
public class ConfigEntity {

    // Getters and Setters
    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 配置组
	 */
	@Column(nullable = false)
	private String configGroup;

	/**
	 * 配置子组
	 */
	@Column(nullable = false)
	private String configSubGroup;

	/**
	 * 配置键
	 */
	@Column(nullable = false)
	private String configKey;

	/**
	 * 配置项完整路径
	 */
	@Column(nullable = false, unique = true)
	private String configPath;

	/**
	 * 配置值
	 */
	@Column(columnDefinition = "TEXT")
	private String configValue;

	/**
	 * 默认值
	 */
	@Column(columnDefinition = "TEXT")
	private String defaultValue;

	/**
	 * 配置描述
	 */
	@Column(columnDefinition = "TEXT")
	private String description;

	/**
	 * 输入类型
	 */
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private ConfigInputType inputType;

	/**
	 * 选项JSON字符串 用于存储SELECT类型的选项数据
	 */
	@Column(columnDefinition = "TEXT")
	private String optionsJson;

	/**
	 * 最后更新时间
	 */
	@Column(nullable = false)
	private LocalDateTime updateTime;

	/**
	 * 创建时间
	 */
	@Column(nullable = false)
	private LocalDateTime createTime;

	@PrePersist
	protected void onCreate() {
		createTime = LocalDateTime.now();
		updateTime = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updateTime = LocalDateTime.now();
	}

}
