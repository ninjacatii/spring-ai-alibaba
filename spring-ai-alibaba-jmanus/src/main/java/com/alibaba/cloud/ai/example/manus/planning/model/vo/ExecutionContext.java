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
package com.alibaba.cloud.ai.example.manus.planning.model.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 执行上下文类，用于在计划的创建、执行和总结过程中传递和维护状态信息。 该类作为计划执行流程中的核心数据载体，在
 * {@link com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator}
 * 的各个阶段之间传递。
 * 主要职责： - 存储计划ID和计划实体信息 - 保存用户原始请求 - 维护计划执行状态 - 存储执行结果摘要 - 控制是否需要生成执行总结
 *
 * @see com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionPlan
 * @see com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator
 */
@Setter
@Getter
public class ExecutionContext {
	private String planId;
	private ExecutionPlan plan;
	private String userRequest;
	private String resultSummary;
	private boolean needSummary;
	private boolean success = false;

	public void updateContext(ExecutionContext context) {
		this.plan = context.getPlan();
		this.userRequest = context.getUserRequest();
		this.resultSummary = context.getResultSummary();
	}

}
