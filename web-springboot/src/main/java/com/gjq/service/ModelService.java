package com.gjq.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gjq.dto.model.ModelAddDTO;
import com.gjq.dto.model.ModelQueryDTO;
import com.gjq.dto.model.ModelUpdateDTO;
import com.gjq.entity.Model;
import com.gjq.vo.model.ModelVO;

import java.util.List;
import java.util.Map;

/**
 * 模型服务接口
 */
public interface ModelService extends IService<Model> {
    
    /**
     * 添加模型
     *
     * @param dto 模型添加DTO
     * @param userId 用户ID
     * @return 模型ID
     */
    Long addModel(ModelAddDTO dto, Long userId);
    
    /**
     * 分页查询模型
     *
     * @param dto 查询参数
     * @param userId 用户ID
     * @param isAdmin 是否为管理员
     * @return 分页结果
     */
    Page<ModelVO> getModelPage(ModelQueryDTO dto, Long userId, boolean isAdmin);
    
    /**
     * 获取模型详情
     *
     * @param id 模型ID
     * @param userId 用户ID
     * @return 模型详情
     */
    ModelVO getModelDetail(Long id, Long userId);
    
    /**
     * 获取所有模型
     *
     * @param userId 用户ID
     * @param isAdmin 是否为管理员
     * @return 模型列表
     */
    List<ModelVO> getAllModels(Long userId, boolean isAdmin);
    
    /**
     * 开始训练模型
     *
     * @param id 模型ID
     * @param userId 用户ID
     * @param trainHyperparams 训练超参数(对象)，可为null
     */
    void trainModel(Long id, Long userId, Object trainHyperparams);
    
    /**
     * 更新模型状态
     *
     * @param id 模型ID
     * @param status 状态
     * @param errorMsg 错误信息
     * @param trainMetrics 训练指标(JSON字符串)
     * @param modelBucket 模型存储桶
     * @param modelObjectKey 模型对象键
     * @return 是否成功
     */
    boolean updateModelStatus(Long id, Integer status, String errorMsg, 
                             String trainMetrics, String modelBucket, 
                             String modelObjectKey);
    
    /**
     * 更新模型基本信息
     *
     * @param dto 模型更新DTO
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean updateModel(ModelUpdateDTO dto, Long userId);
    
    /**
     * 删除模型
     *
     * @param id 模型ID
     * @param userId 用户ID
     */
    void deleteModel(Long id, Long userId);
    
    /**
     * 发布模型
     *
     * @param id 模型ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean publishModel(Long id, Long userId);
    
    /**
     * 取消发布模型
     *
     * @param id 模型ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean unpublishModel(Long id, Long userId);
} 