package com.gjq.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjq.dto.feedback.FeedbackCreateDTO;
import com.gjq.dto.feedback.FeedbackVO;
import com.gjq.entity.Feedback;

/**
 * 评估反馈服务接口
 */
public interface FeedbackService {
    /**
     * 创建评估反馈
     *
     * @param createDTO 创建DTO
     * @param userId 用户ID
     * @return 评估反馈ID
     */
    Long createFeedback(FeedbackCreateDTO createDTO, Long userId);
    
    /**
     * 根据ID获取评估反馈
     *
     * @param id 评估反馈ID
     * @param userId 用户ID
     * @return 评估反馈VO
     */
    FeedbackVO getFeedbackById(Long id, Long userId);
    
    /**
     * 分页查询评估反馈
     *
     * @param page 分页参数
     * @param modelId 模型ID
     * @param userId 用户ID
     * @return 分页结果
     */
    Page<FeedbackVO> pageFeedbacks(Page<Feedback> page, Long modelId, Long userId);
    
    /**
     * 删除评估反馈
     *
     * @param id 评估反馈ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteFeedback(Long id, Long userId);
} 