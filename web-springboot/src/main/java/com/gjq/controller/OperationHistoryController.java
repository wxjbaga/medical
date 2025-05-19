package com.gjq.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjq.common.Result;
import com.gjq.dto.history.OperationHistoryCreateDTO;
import com.gjq.dto.history.OperationHistoryVO;
import com.gjq.entity.OperationHistory;
import com.gjq.service.OperationHistoryService;
import com.gjq.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 操作历史控制器
 */
@Slf4j
@RestController
@RequestMapping("/operation-history")
@Tag(name = "操作历史管理")
public class OperationHistoryController {

    @Autowired
    private OperationHistoryService operationHistoryService;
    
    @PostMapping("/add")
    @Operation(summary = "创建操作历史")
    public Result<Long> createHistory(@RequestBody @Validated OperationHistoryCreateDTO createDTO) {
        Long userId = SecurityUtils.getUserId();
        Long historyId = operationHistoryService.createHistory(createDTO, userId);
        return Result.success(historyId);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取操作历史")
    public Result<OperationHistoryVO> getHistoryById(
            @Parameter(description = "操作历史ID") @PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        OperationHistoryVO vo = operationHistoryService.getHistoryById(id, userId);
        return Result.success(vo);
    }
    
    @GetMapping("/page")
    @Operation(summary = "分页查询操作历史")
    public Result<Page<OperationHistoryVO>> pageHistories(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "模型ID") @RequestParam(required = false) Long modelId,
            @Parameter(description = "创建用户名") @RequestParam(required = false) String createUsername) {
        Long userId = SecurityUtils.getUserId();
        Page<OperationHistory> page = new Page<>(current, size);
        Page<OperationHistoryVO> voPage = operationHistoryService.pageHistories(page, null, modelId, createUsername, userId);
        return Result.success(voPage);
    }
    
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除操作历史")
    public Result<Boolean> deleteHistory(
            @Parameter(description = "操作历史ID") @PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        boolean result = operationHistoryService.deleteHistory(id, userId);
        return Result.success(result);
    }
} 