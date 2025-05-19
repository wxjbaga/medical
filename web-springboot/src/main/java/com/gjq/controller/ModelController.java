package com.gjq.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjq.common.Result;
import com.gjq.dto.model.*;
import com.gjq.service.ModelService;
import com.gjq.utils.SecurityUtils;
import com.gjq.vo.model.ModelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 模型控制器
 */
@Tag(name = "模型管理")
@RestController
@RequestMapping("/model")
public class ModelController {
    
    @Autowired
    private ModelService modelService;
    
    /**
     * 添加模型
     */
    @PostMapping("/add")
    @Operation(summary = "添加模型")
    public Result<Long> addModel(@Valid @RequestBody ModelAddDTO dto) {
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 添加模型
        Long id = modelService.addModel(dto, userId);
        
        return Result.success(id);
    }
    
    /**
     * 分页查询模型
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询模型")
    public Result<Page<ModelVO>> getModelPage(ModelQueryDTO dto) {
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 判断是否为管理员
        boolean isAdmin = SecurityUtils.isAdmin();
        
        // 分页查询模型
        Page<ModelVO> page = modelService.getModelPage(dto, userId, isAdmin);
        
        return Result.success(page);
    }
    
    /**
     * 获取模型详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取模型详情")
    public Result<ModelVO> getModelDetail(
            @Parameter(description = "模型ID") @PathVariable Long id) {
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 获取模型详情
        ModelVO modelVO = modelService.getModelDetail(id, userId);
        
        return Result.success(modelVO);
    }
    
    /**
     * 获取所有模型
     */
    @GetMapping("/list")
    @Operation(summary = "获取所有模型")
    public Result<List<ModelVO>> getAllModels() {
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 判断是否为管理员
        boolean isAdmin = SecurityUtils.isAdmin();
        
        // 获取所有模型
        List<ModelVO> models = modelService.getAllModels(userId, isAdmin);
        
        return Result.success(models);
    }
    
    /**
     * 训练模型
     * @param id 模型ID
     * @param trainParams 训练参数
     * @return 结果
     */
    @PostMapping("/train/{id}")
    @Operation(summary = "开始训练模型")
    public Result<Boolean> trainModel(
            @Parameter(description = "模型ID") @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> trainParams) {
        try {
            // 获取当前用户ID
            Long userId = SecurityUtils.getUserId();
            
            // 获取训练超参数
            Object trainHyperparams = null;
            if (trainParams != null && trainParams.containsKey("trainHyperparams")) {
                trainHyperparams = trainParams.get("trainHyperparams");
            }
            
            // 开始训练模型
            modelService.trainModel(id, userId, trainHyperparams);
            
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 更新模型状态
     */
    @PostMapping("/update-status")
    @Operation(summary = "更新模型状态")
    public Result<Boolean> updateModelStatus(@RequestBody Map<String, Object> params) {
        // 获取参数
        Long id = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        String errorMsg = params.get("errorMsg") != null ? params.get("errorMsg").toString() : null;
        String trainMetrics = params.get("trainMetrics") != null ? params.get("trainMetrics").toString() : null;
        String modelBucket = params.get("modelBucket") != null ? params.get("modelBucket").toString() : null;
        String modelObjectKey = params.get("modelObjectKey") != null ? params.get("modelObjectKey").toString() : null;
        
        // 更新模型状态
        boolean success = modelService.updateModelStatus(id, status, errorMsg, trainMetrics, modelBucket, modelObjectKey);
        
        return Result.success(success);
    }
    
    /**
     * 更新模型基本信息
     */
    @PostMapping("/update")
    @Operation(summary = "更新模型基本信息")
    public Result<Boolean> updateModel(@Valid @RequestBody ModelUpdateDTO dto) {
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 更新模型基本信息
        boolean success = modelService.updateModel(dto, userId);
        
        return Result.success(success);
    }
    
    /**
     * 删除模型
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除模型")
    public Result<Boolean> deleteModel(
            @Parameter(description = "模型ID") @PathVariable Long id) {
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 删除模型
        modelService.deleteModel(id, userId);
        
        return Result.success(true);
    }
    
    /**
     * 发布模型
     */
    @PostMapping("/publish/{id}")
    @Operation(summary = "发布模型")
    public Result<Boolean> publishModel(
            @Parameter(description = "模型ID") @PathVariable Long id) {
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 发布模型
        boolean success = modelService.publishModel(id, userId);
        
        return Result.success(success);
    }
    
    /**
     * 取消发布模型
     */
    @PostMapping("/unpublish/{id}")
    @Operation(summary = "取消发布模型")
    public Result<Boolean> unpublishModel(
            @Parameter(description = "模型ID") @PathVariable Long id) {
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 取消发布模型
        boolean success = modelService.unpublishModel(id, userId);
        
        return Result.success(success);
    }
} 