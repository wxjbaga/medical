package com.gjq.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gjq.client.AlgorithmClient;
import com.gjq.client.FileClient;
import com.gjq.common.StatusConstant;
import com.gjq.common.exception.BusinessException;
import com.gjq.dto.dataset.DatasetAddDTO;
import com.gjq.dto.dataset.DatasetQueryDTO;
import com.gjq.dto.dataset.DatasetUpdateDTO;
import com.gjq.entity.Dataset;
import com.gjq.entity.User;
import com.gjq.mapper.DatasetMapper;
import com.gjq.mapper.UserMapper;
import com.gjq.service.DatasetService;
import com.gjq.utils.SecurityUtils;
import com.gjq.vo.dataset.DatasetVO;
import com.gjq.vo.user.UserInfo;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据集服务实现类
 */
@Slf4j
@Service
public class DatasetServiceImpl extends ServiceImpl<DatasetMapper, Dataset> implements DatasetService {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private FileClient fileClient;
    
    @Autowired
    private AlgorithmClient algorithmClient;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    private final static String datasetBucket = "dataset";
    
    /**
     * 添加数据集
     */
    @Override
    @Transactional
    public Long addDataset(DatasetAddDTO dto, Long userId) {
        
        // 检查数据集名称是否重复
        LambdaQueryWrapper<Dataset> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Dataset::getName, dto.getName());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException("数据集名称已存在");
        }
        
        // 创建数据集
        Dataset dataset = new Dataset();
        dataset.setName(dto.getName());
        dataset.setDescription(dto.getDescription());
        dataset.setStatus(StatusConstant.DATASET_STATUS_UNVERIFIED); // 未验证
        dataset.setTrainCount(0);
        dataset.setValCount(0);
        dataset.setCreateUserId(userId);
        
        // 保存数据集
        baseMapper.insert(dataset);
        
        return dataset.getId();
    }
    
    /**
     * 上传数据集文件
     */
    @Override
    @Transactional
    public Boolean uploadDataset(Long id, MultipartFile file, Long userId) {
        // 获取数据集
        Dataset dataset = getById(id);
        if (dataset == null) {
            throw new BusinessException("数据集不存在");
        }
        
        // 检查权限（管理员可以上传所有，普通用户只能上传自己的）
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!isAdmin && !dataset.getCreateUserId().equals(userId)) {
            throw new BusinessException("您没有权限上传该数据集文件");
        }
        
        // 检查文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new BusinessException("只支持ZIP格式的数据集文件");
        }
        
        try {
            // 删除旧文件（如果存在）
            if (StringUtils.hasText(dataset.getBucket()) && StringUtils.hasText(dataset.getObjectKey())) {
                try {
                    fileClient.delete(dataset.getBucket(), dataset.getObjectKey());
                    log.info("删除旧的数据集文件成功，ID: {}", id);
                } catch (Exception e) {
                    log.warn("删除旧的数据集文件失败，ID: {}, 错误: {}", id, e.getMessage());
                    // 继续执行，不影响上传新文件
                }
            }
            
            // 生成文件对象键
            String objectKey = generateObjectKey(id, userId);
            
            // 保存文件
            File tempFile = File.createTempFile("dataset", ".zip");
            file.transferTo(tempFile);
            
            // 上传到对象存储
            byte[] fileContent = Files.readAllBytes(tempFile.toPath());
            FileClient.UploadResult response = fileClient.upload(datasetBucket, fileContent, objectKey, false);
            
            // 删除临时文件
            tempFile.delete();
            
            // 重置数据集的验证状态和相关信息
            dataset.setBucket(response.getBucket());
            dataset.setObjectKey(response.getObjectKey());
            dataset.setStatus(StatusConstant.DATASET_STATUS_UNVERIFIED); // 未验证
            dataset.setTrainCount(0); // 清空训练样本数量
            dataset.setValCount(0);   // 清空验证样本数量
            dataset.setErrorMsg(null); // 清空错误信息
            
            // 更新数据集
            updateById(dataset);
            
            log.info("上传数据集文件成功，ID: {}", id);
            return true;
            
        } catch (Exception e) {
            log.error("上传数据集文件失败，ID: {}, 错误: {}", id, e.getMessage(), e);
            throw new BusinessException("上传数据集文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 异步验证数据集
     */
    @Override
    @Async("taskExecutor")
    public void validateDatasetAsync(Long id) {
        try {
            log.info("开始异步验证数据集，ID: {}", id);
            
            // 获取数据集
            Dataset dataset = getById(id);
            if (dataset == null) {
                log.error("数据集不存在，ID: {}", id);
                return;
            }
            
            // 调用算法服务验证数据集
            Map<String, Object> params = new HashMap<>();
            params.put("id", dataset.getId());
            
            log.info("调用算法服务验证数据集，ID: {}, 参数: {}", id, params);
            // algorithmClient.post方法已经通过handleResponse处理了响应，
            // 当code==200时，直接返回response.getData()
            Map<String, Object> responseData = algorithmClient.post("/dataset/validate", params);
            log.info("算法服务返回结果: {}", responseData);
            
            // 现在responseData就是算法服务返回的data部分，无需再提取
            if (responseData != null) {
                log.info("验证结果数据: {}", responseData);
                
                try {
                    boolean valid = (boolean) responseData.get("valid");
                    String errorMsg = (String) responseData.get("error_msg");
                    Integer trainCount = responseData.get("train_count") != null ? 
                            ((Number) responseData.get("train_count")).intValue() : 0;
                    Integer valCount = responseData.get("val_count") != null ? 
                            ((Number) responseData.get("val_count")).intValue() : 0;
                    
                    // 更新数据集状态
                    if (valid) {
                        updateDatasetStatus(id, StatusConstant.DATASET_STATUS_VERIFIED_SUCCESS, null, trainCount, valCount);
                        log.info("数据集验证成功，ID: {}, 训练集数量: {}, 验证集数量: {}", id, trainCount, valCount);
                    } else {
                        updateDatasetStatus(id, StatusConstant.DATASET_STATUS_VERIFIED_FAILED, errorMsg, 0, 0);
                        log.info("数据集验证失败，ID: {}，错误: {}", id, errorMsg);
                    }
                } catch (ClassCastException e) {
                    log.error("验证结果数据格式不正确: {}, 错误: {}", responseData, e.getMessage());
                    updateDatasetStatus(id, StatusConstant.DATASET_STATUS_VERIFIED_FAILED, "验证结果数据格式不正确: " + e.getMessage(), 0, 0);
                }
            } else {
                // 如果响应为空，则认为验证失败
                log.error("验证结果为空");
                updateDatasetStatus(id, StatusConstant.DATASET_STATUS_VERIFIED_FAILED, "验证失败，响应数据为空", 0, 0);
            }
            
        } catch (Exception e) {
            log.error("验证数据集时发生错误，ID: {}, 错误: {}", id, e.getMessage(), e);
            updateDatasetStatus(id, StatusConstant.DATASET_STATUS_VERIFIED_FAILED, "验证数据集失败: " + e.getMessage(), 0, 0);
        }
    }
    
    /**
     * 生成对象键
     */
    private String generateObjectKey(Long datasetId, Long userId) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("dataset/%s/%d/%d_%s.zip", dateStr, userId, datasetId, uuid);
    }

    /**
     * 删除数据集
     */
    @Override
    @Transactional
    public void deleteDataset(Long id, Long userId) {
        // 获取数据集
        Dataset dataset = baseMapper.selectById(id);
        if (dataset == null) {
            throw new BusinessException("数据集不存在");
        }
        
        // 检查权限（管理员可以删除所有，普通用户只能删除自己的）
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!isAdmin && !dataset.getCreateUserId().equals(userId)) {
            throw new BusinessException("您没有权限删除该数据集");
        }
        
        // 删除文件
        if (StringUtils.hasText(dataset.getBucket()) && StringUtils.hasText(dataset.getObjectKey())) {
            try {
                fileClient.delete(dataset.getBucket(), dataset.getObjectKey());
            } catch (Exception e) {
                log.error("删除数据集文件失败", e);
            }
        }
        
        // 删除数据集
        baseMapper.deleteById(id);
    }

    /**
     * 获取数据集详情
     */
    @Override
    public DatasetVO getDatasetDetail(Long id, Long userId) {
        // 获取数据集
        Dataset dataset = baseMapper.selectById(id);
        if (dataset == null) {
            throw new BusinessException("数据集不存在");
        }
        
        // 检查权限（管理员可以查看所有，普通用户只能查看自己的）
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!isAdmin && !dataset.getCreateUserId().equals(userId)) {
            throw new BusinessException("您没有权限查看该数据集");
        }
        
        // 转换为VO
        return convertToVO(dataset);
    }

    /**
     * 分页查询数据集
     */
    @Override
    public Page<DatasetVO> getDatasetPage(DatasetQueryDTO dto, Long userId, boolean isAdmin) {
        // 构建查询条件
        LambdaQueryWrapper<Dataset> queryWrapper = new LambdaQueryWrapper<>();
        
        // 根据数据集名称模糊查询
        if (StringUtils.hasText(dto.getName())) {
            queryWrapper.like(Dataset::getName, dto.getName());
        }
        
        // 根据状态查询
        if (dto.getStatus() != null) {
            queryWrapper.eq(Dataset::getStatus, dto.getStatus());
        }
        
        // 根据用户ID查询（管理员可以查看所有，普通用户只能查看自己的）
        if (!isAdmin) {
            queryWrapper.eq(Dataset::getCreateUserId, userId);
        } else if (dto.getCreateUserId() != null) {
            queryWrapper.eq(Dataset::getCreateUserId, dto.getCreateUserId());
        }
        
        // 按创建时间倒序排序
        queryWrapper.orderByDesc(Dataset::getCreateTime);
        
        // 执行分页查询
        Page<Dataset> page = new Page<>(dto.getCurrent(), dto.getSize());
        page = baseMapper.selectPage(page, queryWrapper);
        
        // 转换为VO
        Page<DatasetVO> voPage = new Page<>();
        BeanUtils.copyProperties(page, voPage, "records");
        
        if (page.getRecords() == null || page.getRecords().isEmpty()) {
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }
        
        // 获取相关数据
        List<Long> userIds = page.getRecords().stream()
                .map(Dataset::getCreateUserId)
                .distinct()
                .collect(Collectors.toList());
        
        // 查询用户
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.in(User::getId, userIds);
        List<User> users = userMapper.selectList(userQueryWrapper);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        
        // 转换为VO
        List<DatasetVO> voList = page.getRecords().stream().map(dataset -> {
            DatasetVO vo = new DatasetVO();
            BeanUtils.copyProperties(dataset, vo);
            
            // 设置创建用户
            User user = userMap.get(dataset.getCreateUserId());
            if (user != null) {
                UserInfo userInfo = new UserInfo(user, fileClient);
                vo.setCreateUser(userInfo);
            }
            
            // 设置状态名称
            if (dataset.getStatus() == StatusConstant.DATASET_STATUS_UNVERIFIED) {
                vo.setStatusName("未验证");
            } else if (dataset.getStatus() == StatusConstant.DATASET_STATUS_VERIFYING) {
                vo.setStatusName("验证中");
            } else if (dataset.getStatus() == StatusConstant.DATASET_STATUS_VERIFIED_SUCCESS) {
                vo.setStatusName("验证成功");
            } else if (dataset.getStatus() == StatusConstant.DATASET_STATUS_VERIFIED_FAILED) {
                vo.setStatusName("验证失败");
            }
            
            return vo;
        }).collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 获取所有数据集
     */
    @Override
    public List<DatasetVO> getAllDatasets(Long userId, boolean isAdmin) {
        // 构建查询条件
        LambdaQueryWrapper<Dataset> queryWrapper = new LambdaQueryWrapper<>();
        
        // 根据用户ID查询（管理员可以查看所有，普通用户只能查看自己的）
        if (!isAdmin) {
            queryWrapper.eq(Dataset::getCreateUserId, userId);
        }
        
        // 只查询验证成功的数据集
        queryWrapper.eq(Dataset::getStatus, StatusConstant.DATASET_STATUS_VERIFIED_SUCCESS);
        
        // 按创建时间倒序排序
        queryWrapper.orderByDesc(Dataset::getCreateTime);
        
        // 执行查询
        List<Dataset> datasets = baseMapper.selectList(queryWrapper);
        
        if (datasets == null || datasets.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取相关数据
        List<Long> userIds = datasets.stream()
                .map(Dataset::getCreateUserId)
                .distinct()
                .collect(Collectors.toList());
        
        // 查询用户
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.in(User::getId, userIds);
        List<User> users = userMapper.selectList(userQueryWrapper);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        
        // 转换为VO
        return datasets.stream().map(dataset -> {
            DatasetVO vo = new DatasetVO();
            BeanUtils.copyProperties(dataset, vo);
            
            // 设置创建用户
            User user = userMap.get(dataset.getCreateUserId());
            if (user != null) {
                UserInfo userInfo = new UserInfo(user, fileClient);
                vo.setCreateUser(userInfo);
            }
            
            // 设置状态名称
            if (dataset.getStatus() == StatusConstant.DATASET_STATUS_UNVERIFIED) {
                vo.setStatusName("未验证");
            } else if (dataset.getStatus() == StatusConstant.DATASET_STATUS_VERIFYING) {
                vo.setStatusName("验证中");
            } else if (dataset.getStatus() == StatusConstant.DATASET_STATUS_VERIFIED_SUCCESS) {
                vo.setStatusName("验证成功");
            } else if (dataset.getStatus() == StatusConstant.DATASET_STATUS_VERIFIED_FAILED) {
                vo.setStatusName("验证失败");
            }
            
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 更新数据集验证状态
     */
    @Override
    @Transactional
    public void updateDatasetStatus(Long id, Integer status, String errorMsg, Integer trainCount, Integer valCount) {
        // 获取数据集
        Dataset dataset = baseMapper.selectById(id);
        if (dataset == null) {
            throw new BusinessException("数据集不存在");
        }
        
        // 更新状态
        dataset.setStatus(status);
        dataset.setErrorMsg(errorMsg);
        
        // 如果验证成功，更新样例数量
        if (status == StatusConstant.DATASET_STATUS_VERIFIED_SUCCESS) {
            dataset.setTrainCount(trainCount);
            dataset.setValCount(valCount);
        }
        
        // 更新数据集
        baseMapper.updateById(dataset);
    }
    
    /**
     * 将实体转换为VO
     */
    private DatasetVO convertToVO(Dataset dataset) {
        DatasetVO vo = new DatasetVO();
        BeanUtils.copyProperties(dataset, vo);
        
        // 设置创建用户
        User user = userMapper.selectById(dataset.getCreateUserId());
        if (user != null) {
            UserInfo userInfo = new UserInfo(user, fileClient);
            vo.setCreateUser(userInfo);
        }
        
        // 设置状态名称
        if (dataset.getStatus() == StatusConstant.DATASET_STATUS_UNVERIFIED) {
            vo.setStatusName("未验证");
        } else if (dataset.getStatus() == StatusConstant.DATASET_STATUS_VERIFYING) {
            vo.setStatusName("验证中");
        } else if (dataset.getStatus() == StatusConstant.DATASET_STATUS_VERIFIED_SUCCESS) {
            vo.setStatusName("验证成功");
        } else if (dataset.getStatus() == StatusConstant.DATASET_STATUS_VERIFIED_FAILED) {
            vo.setStatusName("验证失败");
        }
        
        return vo;
    }

    /**
     * 手动验证数据集
     */
    @Override
    @Transactional
    public void validateDataset(Long id, Long userId) {
        // 获取数据集
        Dataset dataset = getById(id);
        if (dataset == null) {
            throw new BusinessException("数据集不存在");
        }
        
        // 检查权限（管理员可以验证所有，普通用户只能验证自己的）
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!isAdmin && !dataset.getCreateUserId().equals(userId)) {
            throw new BusinessException("您没有权限验证该数据集");
        }
        
        // 检查数据集状态
        if (dataset.getStatus() != StatusConstant.DATASET_STATUS_UNVERIFIED && 
            dataset.getStatus() != StatusConstant.DATASET_STATUS_VERIFIED_FAILED) {
            throw new BusinessException("数据集已验证或正在验证中");
        }
        
        // 检查数据集文件是否已上传
        if (!StringUtils.hasText(dataset.getBucket()) || !StringUtils.hasText(dataset.getObjectKey())) {
            throw new BusinessException("请先上传数据集文件");
        }
        
        // 更新数据集状态为验证中
        dataset.setStatus(StatusConstant.DATASET_STATUS_VERIFYING);
        updateById(dataset);
        
        // 在事务提交后异步验证数据集
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    DatasetService datasetService = applicationContext.getBean(DatasetService.class);
                    datasetService.validateDatasetAsync(id);
                }
            });
        } else {
            // 如果不在事务中，直接异步验证
            DatasetService datasetService = applicationContext.getBean(DatasetService.class);
            datasetService.validateDatasetAsync(id);
        }
    }

    /**
     * 更新数据集基本信息
     */
    @Override
    @Transactional
    public Boolean updateDataset(Long id, DatasetUpdateDTO dto, Long userId) {
        // 获取数据集
        Dataset dataset = getById(id);
        if (dataset == null) {
            throw new BusinessException("数据集不存在");
        }
        
        // 检查权限（管理员可以编辑所有，普通用户只能编辑自己的）
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!isAdmin && !dataset.getCreateUserId().equals(userId)) {
            throw new BusinessException("您没有权限编辑该数据集");
        }
        
        // 检查数据集名称是否重复（排除自身）
        LambdaQueryWrapper<Dataset> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Dataset::getName, dto.getName())
                    .ne(Dataset::getId, id);
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException("数据集名称已存在");
        }
        
        // 更新数据集
        dataset.setName(dto.getName());
        dataset.setDescription(dto.getDescription());
        
        // 保存数据集
        boolean success = updateById(dataset);
        
        log.info("更新数据集成功，ID: {}", id);
        return success;
    }
} 