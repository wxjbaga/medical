package com.gjq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjq.client.FileClient;
import com.gjq.common.exception.BusinessException;
import com.gjq.dto.history.OperationHistoryCreateDTO;
import com.gjq.dto.history.OperationHistoryVO;
import com.gjq.entity.Model;
import com.gjq.entity.OperationHistory;
import com.gjq.entity.User;
import com.gjq.mapper.ModelMapper;
import com.gjq.mapper.OperationHistoryMapper;
import com.gjq.mapper.UserMapper;
import com.gjq.service.OperationHistoryService;
import com.gjq.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作历史服务实现类
 */
@Slf4j
@Service
public class OperationHistoryServiceImpl extends ServiceImpl<OperationHistoryMapper, OperationHistory> implements OperationHistoryService {

    @Autowired
    private OperationHistoryMapper operationHistoryMapper;
    
    @Autowired
    private ModelMapper modelMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private FileClient fileClient;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHistory(OperationHistoryCreateDTO createDTO, Long userId) {
        // 验证模型存在
        Model model = modelMapper.selectById(createDTO.getModelId());
        if (model == null) {
            throw new BusinessException("模型不存在");
        }
        
        OperationHistory history = new OperationHistory();
        BeanUtils.copyProperties(createDTO, history);
        history.setCreateUserId(userId);
        
        operationHistoryMapper.insert(history);
        return history.getId();
    }
    
    @Override
    public OperationHistoryVO getHistoryById(Long id, Long userId) {
        OperationHistory history = operationHistoryMapper.selectById(id);
        if (history == null) {
            throw new BusinessException("操作历史不存在");
        }
        
        // 非管理员只能查看自己的操作历史
        if (!SecurityUtils.isAdmin() && !history.getCreateUserId().equals(userId)) {
            throw new BusinessException("无权限查看该操作历史");
        }
        
        return convertToVO(history);
    }
    
    @Override
    public Page<OperationHistoryVO> pageHistories(Page<OperationHistory> page, String operationType, Long modelId, String createUsername, Long userId) {
        LambdaQueryWrapper<OperationHistory> queryWrapper = new LambdaQueryWrapper<>();
        
        // 按模型ID过滤
        if (modelId != null) {
            queryWrapper.eq(OperationHistory::getModelId, modelId);
        }
        
        // 非管理员只能查看自己的操作历史
        if (!SecurityUtils.isAdmin()) {
            queryWrapper.eq(OperationHistory::getCreateUserId, userId);
        } else if (StringUtils.isNotBlank(createUsername)) {
            // 管理员可以按用户名查询
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            userQuery.like(User::getUsername, createUsername);
            List<Long> userIds = userMapper.selectList(userQuery)
                .stream()
                .map(User::getId)
                .collect(Collectors.toList());
            if (!userIds.isEmpty()) {
                queryWrapper.in(OperationHistory::getCreateUserId, userIds);
            } else {
                // 如果没有找到匹配的用户，确保查询结果为空
                queryWrapper.eq(OperationHistory::getCreateUserId, -1L);
            }
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(OperationHistory::getCreateTime);
        
        Page<OperationHistory> historyPage = operationHistoryMapper.selectPage(page, queryWrapper);
        
        // 转换为VO
        Page<OperationHistoryVO> voPage = new Page<>();
        BeanUtils.copyProperties(historyPage, voPage, "records");
        
        List<OperationHistoryVO> voList = historyPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteHistory(Long id, Long userId) {
        OperationHistory history = operationHistoryMapper.selectById(id);
        if (history == null) {
            throw new BusinessException("操作历史不存在");
        }
        
        // 非管理员只能删除自己的操作历史
        if (!SecurityUtils.isAdmin() && !history.getCreateUserId().equals(userId)) {
            throw new BusinessException("无权限删除该操作历史");
        }

        // 删除原始图片
        if (history.getOriginalImageBucket() != null && history.getOriginalImageKey() != null) {
            fileClient.delete(history.getOriginalImageBucket(), history.getOriginalImageKey());
        }
        
        // 删除结果图片 
        if (history.getResultImageBucket() != null && history.getResultImageKey() != null) {
            fileClient.delete(history.getResultImageBucket(), history.getResultImageKey());
        }
        
        // 删除叠加图片 
        if (history.getOverlayImageBucket() != null && history.getOverlayImageKey() != null) {
            fileClient.delete(history.getOverlayImageBucket(), history.getOverlayImageKey());
        }   
        
        return operationHistoryMapper.deleteById(id) > 0;
    }
    
    /**
     * 将实体转换为VO
     */
    private OperationHistoryVO convertToVO(OperationHistory history) {
        OperationHistoryVO vo = new OperationHistoryVO();
        BeanUtils.copyProperties(history, vo);
        
        // 设置模型名称
        Model model = modelMapper.selectById(history.getModelId());
        if (model != null) {
            vo.setModelName(model.getName());
        }
        
        // 设置创建用户名称
        User user = userMapper.selectById(history.getCreateUserId());
        if (user != null) {
            vo.setCreateUserName(user.getUsername());
        }
        
        // 设置图片URL
        if (history.getOriginalImageBucket() != null && history.getOriginalImageKey() != null) {
            vo.setOriginalImageUrl(fileClient.getFileUrl(history.getOriginalImageBucket(), history.getOriginalImageKey()));
        }
        
        if (history.getResultImageBucket() != null && history.getResultImageKey() != null) {
            vo.setResultImageUrl(fileClient.getFileUrl(history.getResultImageBucket(), history.getResultImageKey()));
        }
        
        if (history.getOverlayImageBucket() != null && history.getOverlayImageKey() != null) {
            vo.setOverlayImageUrl(fileClient.getFileUrl(history.getOverlayImageBucket(), history.getOverlayImageKey()));
        }
        
        return vo;
    }
} 