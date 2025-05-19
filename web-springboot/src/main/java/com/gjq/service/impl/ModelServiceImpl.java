package com.gjq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjq.client.AlgorithmClient;
import com.gjq.client.FileClient;
import com.gjq.client.FileClient.UploadResult;
import com.gjq.common.StatusConstant;
import com.gjq.common.exception.BusinessException;
import com.gjq.dto.model.ModelAddDTO;
import com.gjq.dto.model.ModelQueryDTO;
import com.gjq.dto.model.ModelUpdateDTO;
import com.gjq.entity.Dataset;
import com.gjq.entity.Model;
import com.gjq.mapper.DatasetMapper;
import com.gjq.mapper.ModelMapper;
import com.gjq.service.ModelService;
import com.gjq.vo.model.ModelVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.util.StringUtils;
import com.gjq.utils.SecurityUtils;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型服务实现类
 */
@Slf4j
@Service
public class ModelServiceImpl extends ServiceImpl<ModelMapper, Model> implements ModelService {

    @Autowired
    private ModelMapper modelMapper;
    
    @Autowired
    private DatasetMapper datasetMapper;
    
    @Autowired
    private FileClient fileClient;
    
    @Autowired
    private AlgorithmClient algorithmClient;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    private final static String modelDatasetBucket = "model_dataset_copy";
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addModel(ModelAddDTO dto, Long userId) {
        // 检查模型名称是否已存在
        LambdaQueryWrapper<Model> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Model::getName, dto.getName());
        if (count(queryWrapper) > 0) {
            throw new BusinessException("模型名称已存在");
        }
        
        // 检查数据集是否存在
        Dataset dataset = datasetMapper.selectById(dto.getDatasetId());
        if (dataset == null) {
            throw new BusinessException("数据集不存在");
        }
        
        // 检查数据集状态是否为已验证成功
        if (dataset.getStatus() != StatusConstant.DATASET_STATUS_VERIFIED_SUCCESS) {
            throw new BusinessException("数据集未验证或验证失败，无法训练模型");
        }
        
        // 复制数据集文件到模型专属存储
        UploadResult uploadResult = null;
        try {
            byte[] datasetFile = fileClient.get(dataset.getBucket(), dataset.getObjectKey());
            uploadResult = fileClient.upload(
                modelDatasetBucket, 
                datasetFile, 
                "dataset_for_model_" + System.currentTimeMillis() + ".zip", 
                false
            );
        } catch (Exception e) {
            log.error("复制数据集文件失败", e);
            throw new BusinessException("创建模型时复制数据集文件失败: " + e.getMessage());
        }
        
        // 创建模型对象
        Model model = new Model();
        model.setName(dto.getName());
        model.setDescription(dto.getDescription());
        model.setDatasetId(dto.getDatasetId());
        model.setStatus(StatusConstant.MODEL_STATUS_UNTRAINED);
        model.setTrainHyperparams(dto.getTrainHyperparams());
        model.setCreateUserId(userId);
        model.setDatasetBucket(uploadResult.getBucket());
        model.setDatasetObjectKey(uploadResult.getObjectKey());
        model.setCreateTime(LocalDateTime.now());
        model.setUpdateTime(LocalDateTime.now());
        
        // 保存模型
        save(model);
        
        return model.getId();
    }

    @Override
    public Page<ModelVO> getModelPage(ModelQueryDTO dto, Long userId, boolean isAdmin) {
        // 创建分页对象
        Page<ModelVO> page = new Page<>(dto.getCurrent(), dto.getSize());
        
        // 分页查询
        return modelMapper.selectModelPage(page, dto.getName(), dto.getStatus(), dto.getDatasetId(), userId, isAdmin);
    }

    @Override
    public ModelVO getModelDetail(Long id, Long userId) {
        // 获取模型详情
        ModelVO modelVO = modelMapper.selectModelDetail(id);
        
        // 判断是否存在
        if (modelVO == null) {
            throw new BusinessException("模型不存在");
        }
        
        return modelVO;
    }

    @Override
    public List<ModelVO> getAllModels(Long userId, boolean isAdmin) {
        // 获取所有模型
        return modelMapper.selectAllModels(userId, isAdmin);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void trainModel(Long id, Long userId, Object trainHyperparams) {
        // 获取模型
        Model model = getById(id);
        if (model == null) {
            throw new BusinessException("模型不存在");
        }
        
        // 权限校验
        if (!userId.equals(model.getCreateUserId()) && !SecurityUtils.isAdmin()) {
            throw new BusinessException("无权操作此模型");
        }
        
        // 检查模型状态
        if (model.getStatus().equals(StatusConstant.MODEL_STATUS_TRAINING)) {
            throw new BusinessException("模型正在训练中，请勿重复操作");
        }
        
        // 检查数据集关联
        if (model.getDatasetId() == null) {
            throw new BusinessException("模型未关联数据集，无法训练");
        }
        
        // 获取数据集信息
        Dataset dataset = datasetMapper.selectById(model.getDatasetId());
        if (dataset == null) {
            throw new BusinessException("关联的数据集不存在");
        }
        
        try {
            // 删除旧的模型权重文件（如果有）
            if (StringUtils.hasText(model.getModelBucket()) && StringUtils.hasText(model.getModelObjectKey())) {
                fileClient.delete(model.getModelBucket(), model.getModelObjectKey());
            }
            
            // 处理训练超参数
            if (trainHyperparams != null) {
                // 将对象转换为JSON字符串
                String trainHyperparamsJson = null;
                if (trainHyperparams instanceof String) {
                    trainHyperparamsJson = (String) trainHyperparams;
                } else {
                    // 使用JSON库将对象转为字符串
                    try {
                        trainHyperparamsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(trainHyperparams);
                    } catch (Exception e) {
                        log.error("转换训练超参数失败", e);
                        throw new BusinessException("训练超参数格式不正确");
                    }
                }
                model.setTrainHyperparams(trainHyperparamsJson);
            }
            
            // 更新模型状态为训练中，清空错误信息
            LambdaUpdateWrapper<Model> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Model::getId, model.getId())
                        .set(Model::getStatus, StatusConstant.MODEL_STATUS_TRAINING)
                        .set(Model::getErrorMsg, null)
                        .set(Model::getModelBucket, null)
                        .set(Model::getModelObjectKey, null)
                        .set(Model::getTrainMetrics, null)
                        .set(Model::getTrainHyperparams, model.getTrainHyperparams())
                        .set(Model::getUpdateTime, LocalDateTime.now());
            
            update(updateWrapper);
            
            // 获取模型ID，用于在异步任务中使用
            final Long modelId = model.getId();
            
            // 使用TransactionSynchronizationManager确保事务提交后再执行异步任务
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 从Spring上下文中获取当前代理对象bean，然后调用异步方法
                    ModelServiceImpl modelService = applicationContext.getBean(ModelServiceImpl.class);
                    modelService.trainModelAsync(modelId);
                }
            });
            
        } catch (Exception e) {
            log.error("模型训练失败", e);
            // 训练失败，更新模型状态
            model.setStatus(StatusConstant.MODEL_STATUS_TRAINED_FAILED);
            model.setErrorMsg("训练失败：" + e.getMessage());
            model.setUpdateTime(LocalDateTime.now());
            updateById(model);
            throw new BusinessException("启动模型训练失败：" + e.getMessage());
        }
    }

    /**
     * 异步执行模型训练任务
     * 
     * @param modelId 模型ID
     */
    @Async("taskExecutor")
    public void trainModelAsync(Long modelId) {
        try {
            log.info("开始异步训练模型，模型ID: {}", modelId);
            
            // 获取最新的模型信息
            Model model = getById(modelId);
            if (model == null) {
                log.error("模型不存在，模型ID: {}", modelId);
                return;
            }
            
            // 调用算法服务开始训练
            Map<String, Object> params = new HashMap<>();
            params.put("id", model.getId());
            params.put("model_id", model.getId());
            params.put("dataset_id", model.getDatasetId());
            params.put("datasetBucket", model.getDatasetBucket());
            params.put("datasetObjectKey", model.getDatasetObjectKey());
            if (StringUtils.hasText(model.getTrainHyperparams())) {
                params.put("hyperparams", model.getTrainHyperparams());
            }
            
            // 调用算法服务
            algorithmClient.post("/model/train", params);
            log.info("模型训练请求已发送，模型ID: {}", modelId);
            
        } catch (Exception e) {
            log.error("异步训练模型失败，模型ID: {}, 错误信息: {}", modelId, e.getMessage(), e);
            // 更新模型状态为训练失败
            try {
                Model model = getById(modelId);
                if (model != null) {
                    LambdaUpdateWrapper<Model> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.eq(Model::getId, modelId)
                                .set(Model::getStatus, StatusConstant.MODEL_STATUS_TRAINED_FAILED)
                                .set(Model::getErrorMsg, "训练失败：" + e.getMessage())
                                .set(Model::getUpdateTime, LocalDateTime.now());
                    update(updateWrapper);
                }
            } catch (Exception ex) {
                log.error("更新模型状态失败，模型ID: {}, 错误信息: {}", modelId, ex.getMessage(), ex);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateModelStatus(Long id, Integer status, String errorMsg, 
                                   String trainMetrics, String modelBucket, 
                                   String modelObjectKey) {
        // 获取模型信息
        Model model = getById(id);
        if (model == null) {
            log.error("更新模型状态失败: 模型不存在, id={}", id);
            return false;
        }
        
        // 使用UpdateWrapper来更新，确保null值也能被更新
        LambdaUpdateWrapper<Model> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Model::getId, id)
                    .set(Model::getStatus, status)
                    .set(Model::getErrorMsg, errorMsg)
                    .set(Model::getTrainMetrics, trainMetrics)
                    .set(Model::getModelBucket, modelBucket)
                    .set(Model::getModelObjectKey, modelObjectKey)
                    .set(Model::getUpdateTime, LocalDateTime.now());
        
        // 更新模型
        return update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateModel(ModelUpdateDTO dto, Long userId) {
        // 获取模型信息
        Model model = getById(dto.getId());
        if (model == null) {
            throw new BusinessException("模型不存在");
        }
        
        // 检查操作权限
        if (!model.getCreateUserId().equals(userId)) {
            throw new BusinessException("无权操作该模型");
        }
        
        // 如果更新名称，检查名称是否已存在
        if (dto.getName() != null && !dto.getName().equals(model.getName())) {
            LambdaQueryWrapper<Model> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Model::getName, dto.getName());
            queryWrapper.ne(Model::getId, dto.getId()); // 排除自身
            if (count(queryWrapper) > 0) {
                throw new BusinessException("模型名称已存在");
            }
        }
        
        // 处理数据集更新
        if (dto.getDatasetId() != null && !dto.getDatasetId().equals(model.getDatasetId())) {
            // 检查新数据集是否存在
            Dataset newDataset = datasetMapper.selectById(dto.getDatasetId());
            if (newDataset == null) {
                throw new BusinessException("新数据集不存在");
            }
            
            // 检查新数据集状态是否为已验证成功
            if (newDataset.getStatus() != StatusConstant.DATASET_STATUS_VERIFIED_SUCCESS) {
                throw new BusinessException("新数据集未验证或验证失败，无法使用");
            }
            
            // 删除旧的数据集文件
            if (StringUtils.hasText(model.getDatasetBucket()) && StringUtils.hasText(model.getDatasetObjectKey())) {
                try {
                    fileClient.delete(model.getDatasetBucket(), model.getDatasetObjectKey());
                } catch (Exception e) {
                    log.error("删除旧数据集文件失败", e);
                    throw new BusinessException("删除旧数据集文件失败: " + e.getMessage());
                }
            }
            
            // 复制新数据集文件到模型专属存储
            UploadResult uploadResult = null;
            try {
                byte[] datasetFile = fileClient.get(newDataset.getBucket(), newDataset.getObjectKey());
                uploadResult = fileClient.upload(
                    modelDatasetBucket, 
                    datasetFile, 
                    "dataset_for_model_" + System.currentTimeMillis() + ".zip", 
                    false
                );
            } catch (Exception e) {
                log.error("复制新数据集文件失败", e);
                throw new BusinessException("复制新数据集文件失败: " + e.getMessage());
            }
            
            // 删除模型权重文件
            if (StringUtils.hasText(model.getModelBucket()) && StringUtils.hasText(model.getModelObjectKey())) {
                try {
                    fileClient.delete(model.getModelBucket(), model.getModelObjectKey());
                } catch (Exception e) {
                    log.error("删除模型权重文件失败", e);
                    // 不抛出异常，继续执行
                }
            }
            
            // 创建更新包装器
            LambdaUpdateWrapper<Model> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Model::getId, model.getId())
                        .set(Model::getDatasetId, dto.getDatasetId())
                        .set(Model::getDatasetBucket, uploadResult.getBucket())
                        .set(Model::getDatasetObjectKey, uploadResult.getObjectKey())
                        .set(Model::getStatus, StatusConstant.MODEL_STATUS_UNTRAINED)
                        .set(Model::getErrorMsg, null)
                        .set(Model::getTrainHyperparams, null)
                        .set(Model::getTrainMetrics, null)
                        .set(Model::getModelBucket, null)
                        .set(Model::getModelObjectKey, null);
            
            // 如果有基本信息更新，也一并更新
            if (dto.getName() != null) {
                updateWrapper.set(Model::getName, dto.getName());
            }
            if (dto.getDescription() != null) {
                updateWrapper.set(Model::getDescription, dto.getDescription());
            }
            
            updateWrapper.set(Model::getUpdateTime, LocalDateTime.now());
            
            // 执行更新
            return update(updateWrapper);
        }
        
        // 如果只更新基本信息
        if (dto.getName() != null || dto.getDescription() != null) {
            LambdaUpdateWrapper<Model> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Model::getId, model.getId());
            
            if (dto.getName() != null) {
                updateWrapper.set(Model::getName, dto.getName());
            }
            if (dto.getDescription() != null) {
                updateWrapper.set(Model::getDescription, dto.getDescription());
            }
            
            updateWrapper.set(Model::getUpdateTime, LocalDateTime.now());
            
            // 执行更新
            return update(updateWrapper);
        }
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(Long id, Long userId) {
        // 获取模型信息
        Model model = getById(id);
        if (model == null) {
            throw new BusinessException("模型不存在");
        }
        
        // 检查操作权限
        if (!model.getCreateUserId().equals(userId)) {
            throw new BusinessException("无权操作该模型");
        }
        
        // 删除模型文件
        try {
            if (model.getModelBucket() != null && model.getModelObjectKey() != null) {
                fileClient.delete(model.getModelBucket(), model.getModelObjectKey());
            }
            
            // 删除为模型复制的数据集文件
            if (model.getDatasetBucket() != null && model.getDatasetObjectKey() != null) {
                fileClient.delete(model.getDatasetBucket(), model.getDatasetObjectKey());
            }
        } catch (Exception e) {
            log.error("删除模型文件失败", e);
            // 继续执行，不影响数据库记录的删除
        }
        
        // 删除模型记录
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishModel(Long id, Long userId) {
        // 获取模型信息
        Model model = getById(id);
        if (model == null) {
            throw new BusinessException("模型不存在");
        }
        
        // 检查操作权限
        if (!model.getCreateUserId().equals(userId)) {
            throw new BusinessException("无权操作该模型");
        }
        
        // 检查模型状态
        if (model.getStatus() != StatusConstant.MODEL_STATUS_TRAINED_SUCCESS) {
            throw new BusinessException("只有训练成功的模型才能发布");
        }
        
        // 更新状态为已发布
        model.setStatus(StatusConstant.MODEL_STATUS_PUBLISHED);
        
        // 更新模型
        return updateById(model);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unpublishModel(Long id, Long userId) {
        // 获取模型信息
        Model model = getById(id);
        if (model == null) {
            throw new BusinessException("模型不存在");
        }
        
        // 检查操作权限
        if (!model.getCreateUserId().equals(userId)) {
            throw new BusinessException("无权操作该模型");
        }
        
        // 检查模型状态
        if (model.getStatus() != StatusConstant.MODEL_STATUS_PUBLISHED) {
            throw new BusinessException("只有已发布的模型才能取消发布");
        }
        
        // 更新状态为训练成功
        model.setStatus(StatusConstant.MODEL_STATUS_TRAINED_SUCCESS);
        
        // 更新模型
        return updateById(model);
    }
} 