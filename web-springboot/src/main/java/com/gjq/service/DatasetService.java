package com.gjq.service;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gjq.dto.dataset.DatasetAddDTO;
import com.gjq.dto.dataset.DatasetQueryDTO;
import com.gjq.dto.dataset.DatasetUpdateDTO;
import com.gjq.entity.Dataset;
import com.gjq.vo.dataset.DatasetVO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 数据集服务接口
 */
public interface DatasetService extends IService<Dataset> {
    
    /**
     * 添加数据集
     * 
     * @param dto 数据集添加DTO
     * @param userId 用户ID
     * @return 数据集ID
     */
    Long addDataset(DatasetAddDTO dto, Long userId);
    
    /**
     * 上传数据集文件
     * 
     * @param id 数据集ID
     * @param file 文件
     * @param userId 用户ID
     * @return 是否上传成功
     */
    Boolean uploadDataset(Long id, MultipartFile file, Long userId);
    
    /**
     * 异步验证数据集
     * 
     * @param id 数据集ID
     */
    void validateDatasetAsync(Long id);
    
    /**
     * 删除数据集
     * 
     * @param id 数据集ID
     * @param userId 用户ID
     */
    void deleteDataset(Long id, Long userId);
    
    /**
     * 获取数据集详情
     * 
     * @param id 数据集ID
     * @param userId 用户ID
     * @return 数据集VO
     */
    DatasetVO getDatasetDetail(Long id, Long userId);
    
    /**
     * 分页查询数据集
     * 
     * @param dto 查询条件
     * @param userId 用户ID
     * @param isAdmin 是否为管理员
     * @return 分页结果
     */
    Page<DatasetVO> getDatasetPage(DatasetQueryDTO dto, Long userId, boolean isAdmin);
    
    /**
     * 获取所有数据集
     * 
     * @param userId 用户ID
     * @param isAdmin 是否为管理员
     * @return 数据集列表
     */
    List<DatasetVO> getAllDatasets(Long userId, boolean isAdmin);
    
    /**
     * 更新数据集验证状态
     * 
     * @param id 数据集ID
     * @param status 状态(0:未验证 1:验证中 2:验证成功 3:验证失败)
     * @param errorMsg 错误消息
     * @param trainCount 训练样例数量
     * @param valCount 验证样例数量
     */
    void updateDatasetStatus(Long id, Integer status, String errorMsg, Integer trainCount, Integer valCount);
    
    /**
     * 手动验证数据集
     * 
     * @param id 数据集ID
     * @param userId 用户ID
     */
    void validateDataset(Long id, Long userId);

    /**
     * 更新数据集基本信息
     * 
     * @param id 数据集ID
     * @param dto 数据集更新DTO
     * @param userId 用户ID
     * @return 是否更新成功
     */
    Boolean updateDataset(Long id, DatasetUpdateDTO dto, Long userId);
} 