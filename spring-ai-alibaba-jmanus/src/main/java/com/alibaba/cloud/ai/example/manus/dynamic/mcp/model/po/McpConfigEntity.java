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
package com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "mcp_config")
public class McpConfigEntity {

    // Getters and Setters
    @Setter
    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String mcpServerName;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private McpConfigType connectionType;

	@Column(nullable = false, length = 4000)
	private String connectionConfig;

    public McpConfigEntity setMcpServerName(String mcpServerName) {
		this.mcpServerName = mcpServerName;
		return this;
	}

    public McpConfigEntity setConnectionType(McpConfigType connectionType) {
		this.connectionType = connectionType;
		return this;
	}

    public McpConfigEntity setConnectionConfig(String connectionConfig) {
		this.connectionConfig = connectionConfig;
		return this;
	}

	@Override
	public String toString() {
		return "McpConfigEntity{" + "id=" + id + ", mcpServerName='" + mcpServerName + '\'' + ", connectionType="
				+ connectionType + ", connectionConfig='" + connectionConfig + '\'' + '}';
	}

}
