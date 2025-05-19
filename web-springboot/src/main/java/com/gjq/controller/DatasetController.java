package com.gjq.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjq.client.AlgorithmClient;
import com.gjq.client.FileClient;
import com.gjq.common.Result;
import com.gjq.common.StatusConstant;
import com.gjq.dto.dataset.DatasetAddDTO;
import com.gjq.dto.dataset.DatasetQueryDTO;
import com.gjq.dto.dataset.DatasetUpdateDTO;
import com.gjq.entity.Dataset;
import com.gjq.entity.User;
import com.gjq.service.DatasetService;
import com.gjq.utils.SecurityUtils;
import com.gjq.vo.dataset.DatasetVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据集控制器
 */
@Slf4j
@Tag(name = "数据集管理")
@RestController
@RequestMapping("/dataset")
public class DatasetController {
    
    @Autowired
    private DatasetService datasetService;
    
    /**
     * 添加数据集
     */
    @PostMapping("/add")
    @Operation(summary = "添加数据集")
    public Result<Long> addDataset(@Valid @RequestBody DatasetAddDTO dto) {
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 添加数据集
        Long id = datasetService.addDataset(dto, userId);
        
        return Result.success(id);
    }
    
    /**
     * 上传数据集文件
     */
    @PostMapping("/upload/{id}")
    @Operation(summary = "上传数据集文件")
    public Result<Boolean> uploadDataset(
            @Parameter(description = "数据集ID") @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 上传数据集文件
        Boolean success = datasetService.uploadDataset(id, file, userId);
        
        return Result.success(success);
    }
    
    /**
     * 删除数据集
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除数据集")
    public Result<Boolean> deleteDataset(
            @Parameter(description = "数据集ID") @PathVariable Long id) {
        
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 删除数据集
        datasetService.deleteDataset(id, userId);
        
        return Result.success(true);
    }
    
    /**
     * 获取数据集详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取数据集详情")
    public Result<DatasetVO> getDatasetDetail(
            @Parameter(description = "数据集ID") @PathVariable Long id) {
        
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 获取数据集详情
        DatasetVO dataset = datasetService.getDatasetDetail(id, userId);
        
        return Result.success(dataset);
    }
    
    /**
     * 分页查询数据集
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询数据集")
    public Result<Page<DatasetVO>> getDatasetPage(DatasetQueryDTO dto) {
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 判断是否为管理员
        boolean isAdmin = SecurityUtils.isAdmin();
        
        // 分页查询数据集
        Page<DatasetVO> page = datasetService.getDatasetPage(dto, userId, isAdmin);
        
        return Result.success(page);
    }
    
    /**
     * 获取所有数据集
     */
    @GetMapping("/list")
    @Operation(summary = "获取所有数据集")
    public Result<List<DatasetVO>> getAllDatasets() {
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 判断是否为管理员
        boolean isAdmin = SecurityUtils.isAdmin();
        
        // 获取所有数据集
        List<DatasetVO> datasets = datasetService.getAllDatasets(userId, isAdmin);
        
        return Result.success(datasets);
    }
    
    /**
     * 验证数据集
     */
    @PostMapping("/validate/{id}")
    @Operation(summary = "验证数据集")
    public Result<Boolean> validateDataset(
            @Parameter(description = "数据集ID") @PathVariable Long id) {
        
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 验证数据集
        datasetService.validateDataset(id, userId);
        
        return Result.success(true);
    }
    
    /**
     * 更新数据集状态
     */
    @PostMapping("/update-status")
    @Operation(summary = "更新数据集状态")
    public Result<Boolean> updateDatasetStatus(@RequestBody Map<String, Object> params) {
        // 获取参数
        Long id = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        String errorMsg = params.get("errorMsg") != null ? params.get("errorMsg").toString() : null;
        Integer trainCount = params.get("trainCount") != null ? Integer.valueOf(params.get("trainCount").toString()) : 0;
        Integer valCount = params.get("valCount") != null ? Integer.valueOf(params.get("valCount").toString()) : 0;
        
        // 更新数据集状态
        datasetService.updateDatasetStatus(id, status, errorMsg, trainCount, valCount);
        
        return Result.success(true);
    }

    /**
     * 更新数据集基本信息
     */
    @PutMapping("/update/{id}")
    @Operation(summary = "更新数据集")
    public Result<Boolean> updateDataset(
            @Parameter(description = "数据集ID") @PathVariable Long id,
            @Valid @RequestBody DatasetUpdateDTO dto) {
        
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 更新数据集
        Boolean success = datasetService.updateDataset(id, dto, userId);
        
        return Result.success(success);
    }
} 