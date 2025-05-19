package com.gjq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjq.client.FileClient;
import com.gjq.common.exception.BusinessException;
import com.gjq.dto.feedback.FeedbackCreateDTO;
import com.gjq.dto.feedback.FeedbackVO;
import com.gjq.entity.Feedback;
import com.gjq.entity.Model;
import com.gjq.entity.User;
import com.gjq.mapper.FeedbackMapper;
import com.gjq.mapper.ModelMapper;
import com.gjq.mapper.UserMapper;
import com.gjq.service.FeedbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评估反馈服务实现类
 */
@Slf4j
@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {

    @Autowired
    private FeedbackMapper feedbackMapper;
    
    @Autowired
    private ModelMapper modelMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private FileClient fileClient;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFeedback(FeedbackCreateDTO createDTO, Long userId) {
        // 验证模型存在
        Model model = modelMapper.selectById(createDTO.getModelId());
        if (model == null) {
            throw new BusinessException("模型不存在");
        }
        
        Feedback feedback = new Feedback();
        BeanUtils.copyProperties(createDTO, feedback);
        feedback.setCreateUserId(userId);
        
        feedbackMapper.insert(feedback);
        return feedback.getId();
    }
    
    @Override
    public FeedbackVO getFeedbackById(Long id, Long userId) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BusinessException("评估反馈不存在");
        }
        
        // 非管理员只能查看自己的评估反馈
        boolean isAdmin = isAdmin(userId);
        if (!isAdmin && !feedback.getCreateUserId().equals(userId)) {
            throw new BusinessException("无权限查看该评估反馈");
        }
        
        return convertToVO(feedback);
    }
    
    @Override
    public Page<FeedbackVO> pageFeedbacks(Page<Feedback> page, Long modelId, Long userId) {
        LambdaQueryWrapper<Feedback> queryWrapper = new LambdaQueryWrapper<>();
        
        // 按模型ID过滤
        if (modelId != null) {
            queryWrapper.eq(Feedback::getModelId, modelId);
        }
        
        // 非管理员只能查看自己的评估反馈
        boolean isAdmin = isAdmin(userId);
        if (!isAdmin) {
            queryWrapper.eq(Feedback::getCreateUserId, userId);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Feedback::getCreateTime);
        
        Page<Feedback> feedbackPage = feedbackMapper.selectPage(page, queryWrapper);
        
        // 转换为VO
        Page<FeedbackVO> voPage = new Page<>();
        BeanUtils.copyProperties(feedbackPage, voPage, "records");
        
        List<FeedbackVO> voList = feedbackPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFeedback(Long id, Long userId) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BusinessException("评估反馈不存在");
        }
        
        // 非管理员只能删除自己的评估反馈
        boolean isAdmin = isAdmin(userId);
        if (!isAdmin && !feedback.getCreateUserId().equals(userId)) {
            throw new BusinessException("无权限删除该评估反馈");
        }

        // 删除原始图片
        if (feedback.getOriginalImageBucket() != null && feedback.getOriginalImageKey() != null) {
            fileClient.delete(feedback.getOriginalImageBucket(), feedback.getOriginalImageKey());
        }
        
        // 删除标签图片 
        if (feedback.getLabelImageBucket() != null && feedback.getLabelImageKey() != null) {
            fileClient.delete(feedback.getLabelImageBucket(), feedback.getLabelImageKey());
        }
        
        // 删除叠加图片 
        if (feedback.getOverlayImageBucket() != null && feedback.getOverlayImageKey() != null) {
            fileClient.delete(feedback.getOverlayImageBucket(), feedback.getOverlayImageKey());
        }
        
        return feedbackMapper.deleteById(id) > 0;
    }
    
    /**
     * 将实体转换为VO
     */
    private FeedbackVO convertToVO(Feedback feedback) {
        FeedbackVO vo = new FeedbackVO();
        BeanUtils.copyProperties(feedback, vo);
        
        // 设置模型名称
        Model model = modelMapper.selectById(feedback.getModelId());
        if (model != null) {
            vo.setModelName(model.getName());
        }
        
        // 设置创建用户名称
        User user = userMapper.selectById(feedback.getCreateUserId());
        if (user != null) {
            vo.setCreateUserName(user.getUsername());
        }
        
        // 设置图片URL
        if (feedback.getOriginalImageBucket() != null && feedback.getOriginalImageKey() != null) {
            vo.setOriginalImageUrl(fileClient.getFileUrl(feedback.getOriginalImageBucket(), feedback.getOriginalImageKey()));
        }
        
        if (feedback.getLabelImageBucket() != null && feedback.getLabelImageKey() != null) {
            vo.setLabelImageUrl(fileClient.getFileUrl(feedback.getLabelImageBucket(), feedback.getLabelImageKey()));
        }
        
        if (feedback.getOverlayImageBucket() != null && feedback.getOverlayImageKey() != null) {
            vo.setOverlayImageUrl(fileClient.getFileUrl(feedback.getOverlayImageBucket(), feedback.getOverlayImageKey()));
        }
        
        return vo;
    }
    
    /**
     * 判断用户是否为管理员
     */
    private boolean isAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && "admin".equals(user.getRole());
    }
} 