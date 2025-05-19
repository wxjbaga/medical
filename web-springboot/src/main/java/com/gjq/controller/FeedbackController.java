package com.gjq.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjq.common.Result;
import com.gjq.dto.feedback.FeedbackCreateDTO;
import com.gjq.dto.feedback.FeedbackVO;
import com.gjq.entity.Feedback;
import com.gjq.service.FeedbackService;
import com.gjq.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 评估反馈控制器
 */
@Slf4j
@RestController
@RequestMapping("/feedback")
@Tag(name = "评估反馈管理")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;
    
    @PostMapping("/add")
    @Operation(summary = "创建评估反馈")
    public Result<Long> createFeedback(@RequestBody @Validated FeedbackCreateDTO createDTO) {
        Long userId = SecurityUtils.getUserId();
        Long feedbackId = feedbackService.createFeedback(createDTO, userId);
        return Result.success(feedbackId);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取评估反馈")
    public Result<FeedbackVO> getFeedbackById(
            @Parameter(description = "评估反馈ID") @PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        FeedbackVO vo = feedbackService.getFeedbackById(id, userId);
        return Result.success(vo);
    }
    
    @GetMapping("/page")
    @Operation(summary = "分页查询评估反馈")
    public Result<Page<FeedbackVO>> pageFeedbacks(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "模型ID") @RequestParam(required = false) Long modelId) {
        Long userId = SecurityUtils.getUserId();
        Page<Feedback> page = new Page<>(current, size);
        Page<FeedbackVO> voPage = feedbackService.pageFeedbacks(page, modelId, userId);
        return Result.success(voPage);
    }
    
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除评估反馈")
    public Result<Boolean> deleteFeedback(
            @Parameter(description = "评估反馈ID") @PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        boolean result = feedbackService.deleteFeedback(id, userId);
        return Result.success(result);
    }
} 