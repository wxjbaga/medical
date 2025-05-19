package com.gjq.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gjq.dto.history.OperationHistoryCreateDTO;
import com.gjq.dto.history.OperationHistoryVO;
import com.gjq.entity.OperationHistory;

/**
 * 操作历史服务接口
 */
public interface OperationHistoryService extends IService<OperationHistory> {
    /**
     * 创建操作历史
     *
     * @param createDTO 创建DTO
     * @param userId 用户ID
     * @return 操作历史ID
     */
    Long createHistory(OperationHistoryCreateDTO createDTO, Long userId);
    
    /**
     * 根据ID获取操作历史
     *
     * @param id 操作历史ID
     * @param userId 当前用户ID
     * @return 操作历史VO
     */
    OperationHistoryVO getHistoryById(Long id, Long userId);
    
    /**
     * 分页查询操作历史
     *
     * @param page 分页参数
     * @param operationType 操作类型
     * @param modelId 模型ID
     * @param createUsername 创建用户名
     * @param userId 当前用户ID
     * @return 分页结果
     */
    Page<OperationHistoryVO> pageHistories(Page<OperationHistory> page, String operationType, Long modelId, String createUsername, Long userId);
    
    /**
     * 删除操作历史
     *
     * @param id 操作历史ID
     * @param userId 当前用户ID
     * @return 是否成功
     */
    boolean deleteHistory(Long id, Long userId);
} 