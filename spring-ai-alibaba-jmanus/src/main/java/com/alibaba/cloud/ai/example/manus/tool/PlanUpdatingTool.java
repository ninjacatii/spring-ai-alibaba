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
package com.alibaba.cloud.ai.example.manus.tool;

import com.alibaba.cloud.ai.example.manus.agent.AgentState;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionPlan;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi.FunctionTool;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class PlanUpdatingTool implements Function<String, ToolExecuteResult> {
	@Getter
	private ExecutionPlan currentPlan;

	private static final String PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "command": {
			            "description": "update a execution plan , Available commands: update",
			            "enum": [
			                "update"
			            ],
			            "type": "string"
			        },
			        "steps": {
			            "description": "List of subsequent plan steps",
			            "type": "array",
			            "items": {
			                "type": "string"
			            }
			        },
			        "updating_status": {
			            "description": "Status to updating",
			            "enum": ["unchanged", "updated"],
			            "type": "string"
			        }
			    },
			    "required": ["command", "update_status"]
			}
			""";

	private static final String name = "plan_updating";

	private static final String description = "Plan updating tool";

	public FunctionTool getToolDefinition() {
		return new FunctionTool(new FunctionTool.Function(description, name, PARAMETERS));
	}

	public FunctionToolCallback getFunctionToolCallback() {
		return FunctionToolCallback.builder(name, this)
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
			.build();
	}

	public ToolExecuteResult run(String toolInput) {
		try {
			Map<String, Object> input = JSON.parseObject(toolInput, new TypeReference<>() {
			});
			String command = (String) input.get("command");
			String updatingStatus = (String) input.get("updating_status");
			List<String> steps = JSON.parseObject(JSON.toJSONString(input.get("steps")),
					new TypeReference<>() {
					});

			return switch (command) {
				case "update" -> updatePlan(updatingStatus, steps);
				default -> {
					log.info("收到无效的命令: {}", command);
					throw new IllegalArgumentException("Invalid command: " + command);
				}
			};
		} catch (Exception e) {
			log.info("执行更新计划工具时发生错误", e);
			return new ToolExecuteResult("Error executing planning tool: " + e.getMessage());
		}
	}

	/**
	 * 创建单个执行步骤
	 * @param step 步骤描述
	 * @param index 步骤索引
	 * @return 创建的ExecutionStep实例
	 */
	private ExecutionStep createExecutionStep(String step, int index) {
		ExecutionStep executionStep = new ExecutionStep();
		executionStep.setStepIndex(index);
		executionStep.setStepRequirement(step);
		return executionStep;
	}

	 public ToolExecuteResult updatePlan(String updatingStatus, List<String> steps) {
		if ("unchanged".equals(updatingStatus)) {
			return new ToolExecuteResult("Plan unchanged.");
		} else {
			List<ExecutionStep> list = currentPlan.getSteps();
			while (!list.isEmpty() && list.get(list.size() - 1).getStatus() == AgentState.NOT_STARTED) {
				list.remove(list.size() - 1);
			}
			if (steps != null) {
				for (String step : steps) {
					currentPlan.addStep(createExecutionStep(step, currentPlan.getStepCount() + 1));
				}
			}
			return new ToolExecuteResult("Plan updated: " + currentPlan.getPlanId() + "\n" + currentPlan.getPlanExecutionStateStringFormat(false));
		}
	 }

	@Override
	public ToolExecuteResult apply(String input) {
		return run(input);
	}
}
