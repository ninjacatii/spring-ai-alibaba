package com.alibaba.cloud.ai.example.manus.tool.textOperator;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Map;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class TextFileSaveTool implements ToolCallBiFunctionDef {

    private static final Logger log = LoggerFactory.getLogger(TextFileSaveTool.class);

    private final String workingDirectoryPath;

    private final TextFileService textFileService;

    private String planId;

    public TextFileSaveTool(String workingDirectoryPath, TextFileService textFileService) {
        this.workingDirectoryPath = workingDirectoryPath;
        this.textFileService = textFileService;
    }

    private final String PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "file_path": {
			            "type": "string",
			            "description": "The path where the text file is located or should be saved"
			        },
			        "content": {
			            "type": "string",
			            "description": "The content to write or append to the file"
			        }
			    },
			    "required": ["file_path", "content"]
			}
			""";

    private final String TOOL_NAME = "text_file_saver";

    private final String TOOL_DESCRIPTION = """
			对文本文件（包括 md、html、css、java 等）执行写入文本操作，并保存该文件

			支持的文件类型包括：
			- 文本文件 (.txt)
			- Markdown 文件 (.md, .markdown)
			- 网页文件 (.html, .css, .scss, .sass, .less)
			- 编程文件 (.java, .py, .js, .ts, .jsx, .tsx)
			- 配置文件 (.xml, .json, .yaml, .yml, .properties)
			- 脚本文件 (.sh, .bat, .cmd)
			- 日志文件 (.log)
			- 以及更多基于文本的文件类型
			""";

    public OpenAiApi.FunctionTool getToolDefinition() {
        OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(TOOL_DESCRIPTION, TOOL_NAME,
                PARAMETERS);
        return new OpenAiApi.FunctionTool(function);
    }

    public FunctionToolCallback getFunctionToolCallback(String workingDirectoryPath, TextFileService textFileService) {
        return FunctionToolCallback.builder(TOOL_NAME, new TextFileSaveTool(workingDirectoryPath, textFileService))
                .description(TOOL_DESCRIPTION)
                .inputSchema(PARAMETERS)
                .inputType(String.class)
                .build();
    }

    public ToolExecuteResult run(String toolInput) {
        log.info("TextFileSaveTool toolInput:{}", toolInput);
        try {
            Map<String, Object> toolInputMap = JSON.parseObject(toolInput, new TypeReference<>() {
            });
            String planId = this.planId;

            String filePath = (String) toolInputMap.get("file_path");
            String content = (String) toolInputMap.get("content");

            ToolExecuteResult result = openFile(planId, filePath);
            if (result.getOutput().contains("Error:")) {
                throw new Exception(result.getOutput());
            }
            return saveAndClose(planId, content);
        }
        catch (Exception e) {
            String planId = this.planId;
            textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
                    "Error: " + e.getMessage());
            return new ToolExecuteResult("Error: " + e.getMessage());
        }
    }

    private ToolExecuteResult openFile(String planId, String filePath) {
        try {
            // 检查文件类型
            if (!textFileService.isSupportedFileType(filePath)) {
                textFileService.updateFileState(planId, filePath, "Error: Unsupported file type");
                return new ToolExecuteResult("Unsupported file type. Only text-based files are supported.");
            }

            textFileService.validateAndGetAbsolutePath(workingDirectoryPath, filePath);

            // 如果文件不存在，先创建父目录
            Path absolutePath = Paths.get(workingDirectoryPath).resolve(filePath);
            if (!Files.exists(absolutePath)) {
                try {
                    Files.createDirectories(absolutePath.getParent());
                    Files.createFile(absolutePath);
                    textFileService.updateFileState(planId, filePath, "Success: New file created");
                    return new ToolExecuteResult("New file created successfully: " + absolutePath);
                }
                catch (IOException e) {
                    textFileService.updateFileState(planId, filePath,
                            "Error: Failed to create file: " + e.getMessage());
                    return new ToolExecuteResult("Failed to create file: " + e.getMessage());
                }
            }

            textFileService.updateFileState(planId, filePath, "Success: File opened");
            return new ToolExecuteResult("File opened successfully: " + absolutePath);
        }
        catch (IOException e) {
            textFileService.updateFileState(planId, filePath, "Error: " + e.getMessage());
            return new ToolExecuteResult("Error opening file: " + e.getMessage());
        }
    }

    private ToolExecuteResult replaceText(String planId, String sourceText, String targetText) {
        try {
            String currentFilePath = textFileService.getCurrentFilePath(planId);
            if (currentFilePath.isEmpty()) {
                textFileService.updateFileState(planId, "", "Error: No file is currently open");
                return new ToolExecuteResult("Error: No file is currently open");
            }

            Path absolutePath = Paths.get(workingDirectoryPath).resolve(currentFilePath);
            String content = Files.readString(absolutePath);
            String newContent = content.replace(sourceText, targetText);
            Files.writeString(absolutePath, newContent);

            textFileService.updateFileState(planId, currentFilePath, "Success: Text replaced");
            return new ToolExecuteResult("Text replaced successfully");
        }
        catch (IOException e) {
            textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
                    "Error: " + e.getMessage());
            return new ToolExecuteResult("Error replacing text: " + e.getMessage());
        }
    }

    private ToolExecuteResult getCurrentText(String planId) {
        try {
            String currentFilePath = textFileService.getCurrentFilePath(planId);
            if (currentFilePath.isEmpty()) {
                textFileService.updateFileState(planId, "", "Error: No file is currently open");
                return new ToolExecuteResult("Error: No file is currently open");
            }

            Path absolutePath = Paths.get(workingDirectoryPath).resolve(currentFilePath);
            String content = Files.readString(absolutePath);

            textFileService.updateFileState(planId, currentFilePath, "Success: Retrieved current text");
            return new ToolExecuteResult(content);
        }
        catch (IOException e) {
            textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
                    "Error: " + e.getMessage());
            return new ToolExecuteResult("Error retrieving text: " + e.getMessage());
        }
    }

    private ToolExecuteResult saveAndClose(String planId, String content) {
        try {
            String currentFilePath = textFileService.getCurrentFilePath(planId);
            if (currentFilePath.isEmpty()) {
                textFileService.updateFileState(planId, "", "Error: No file is currently open");
                return new ToolExecuteResult("Error: No file is currently open");
            }
            Path absolutePath = Paths.get(workingDirectoryPath).resolve(currentFilePath);

            if (content != null) {
                Files.writeString(absolutePath, content);
            }

            // 强制刷新到磁盘
            try (FileChannel channel = FileChannel.open(absolutePath, StandardOpenOption.WRITE)) {
                channel.force(true);
            }

            textFileService.updateFileState(planId, "", "Success: File saved and closed");
            textFileService.closeFileForPlan(planId);
            return new ToolExecuteResult("File saved and closed successfully: " + absolutePath);
        }
        catch (IOException e) {
            textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
                    "Error: " + e.getMessage());
            return new ToolExecuteResult("Error saving file: " + e.getMessage());
        }
    }

    private ToolExecuteResult appendToFile(String planId, String content) {
        try {
            if (content == null || content.isEmpty()) {
                textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
                        "Error: No content to append");
                return new ToolExecuteResult("Error: No content to append");
            }

            String currentFilePath = textFileService.getCurrentFilePath(planId);
            if (currentFilePath.isEmpty()) {
                textFileService.updateFileState(planId, "", "Error: No file is currently open");
                return new ToolExecuteResult("Error: No file is currently open");
            }

            Path absolutePath = Paths.get(workingDirectoryPath).resolve(currentFilePath);
            Files.writeString(absolutePath, "\n" + content, StandardOpenOption.APPEND, StandardOpenOption.CREATE);

            textFileService.updateFileState(planId, currentFilePath, "Success: Content appended");
            return new ToolExecuteResult("Content appended successfully");
        }
        catch (IOException e) {
            textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
                    "Error: " + e.getMessage());
            return new ToolExecuteResult("Error appending to file: " + e.getMessage());
        }
    }

    private ToolExecuteResult countWords(String planId) {
        try {
            String currentFilePath = textFileService.getCurrentFilePath(planId);
            if (currentFilePath.isEmpty()) {
                textFileService.updateFileState(planId, "", "Error: No file is currently open");
                return new ToolExecuteResult("Error: No file is currently open");
            }

            Path absolutePath = Paths.get(workingDirectoryPath).resolve(currentFilePath);
            String content = Files.readString(absolutePath);
            int wordCount = content.isEmpty() ? 0 : content.split("\\s+").length;

            textFileService.updateFileState(planId, currentFilePath, "Success: Counted words");
            return new ToolExecuteResult(String.format("Total word count (including Markdown symbols): %d", wordCount));
        }
        catch (IOException e) {
            textFileService.updateFileState(planId, textFileService.getCurrentFilePath(planId),
                    "Error: " + e.getMessage());
            return new ToolExecuteResult("Error counting words: " + e.getMessage());
        }
    }

    @Override
    public void setPlanId(String planId) {
        this.planId = planId;
    }

    @Override
    public String getCurrentToolStateString() {
        String planId = this.planId;
        return String.format("""
				Current Text File Operation State:
				- Working Directory:
				%s

				- Current File:
				%s
				- File Type: %s

				- Last Operation Result:
				%s
				""", workingDirectoryPath,
                textFileService.getCurrentFilePath(planId).isEmpty() ? "No file open"
                        : textFileService.getCurrentFilePath(planId),
                textFileService.getCurrentFilePath(planId).isEmpty() ? "N/A"
                        : textFileService.getFileExtension(textFileService.getCurrentFilePath(planId)),
                textFileService.getLastOperationResult(planId).isEmpty() ? "No operation performed yet"
                        : textFileService.getLastOperationResult(planId));
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return TOOL_DESCRIPTION;
    }

    @Override
    public String getParameters() {
        return PARAMETERS;
    }

    @Override
    public Class<?> getInputType() {
        return String.class;
    }

    @Override
    public boolean isReturnDirect() {
        return false;
    }

    @Override
    public ToolExecuteResult apply(String s, ToolContext toolContext) {
        return run(s);
    }

    @Override
    public void cleanup(String planId) {
        if (planId != null) {
            log.info("Cleaning up text file resources for plan: {}", planId);
            textFileService.closeFileForPlan(planId);
        }
    }

    // @Override
    // public FileState getInstance(String planId) {
    // if (planId == null) {
    // throw new IllegalArgumentException("planId cannot be null");
    // }
    // return textFileService.getFileState(planId);
    // }

    @Override
    public String getServiceGroup() {
        return "default-service-group";
    }

}

