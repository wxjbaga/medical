package com.gjq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjq.entity.Model;
import com.gjq.vo.model.ModelVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模型Mapper接口
 */
@Mapper
public interface ModelMapper extends BaseMapper<Model> {
    
    /**
     * 分页查询模型
     *
     * @param page 分页参数
     * @param name 模型名称
     * @param status 状态
     * @param datasetId 数据集ID
     * @param userId 用户ID
     * @param isAdmin 是否为管理员
     * @return 分页结果
     */
    Page<ModelVO> selectModelPage(
            Page<ModelVO> page,
            @Param("name") String name,
            @Param("status") Integer status,
            @Param("datasetId") Long datasetId,
            @Param("userId") Long userId,
            @Param("isAdmin") boolean isAdmin);
    
    /**
     * 获取模型详情
     *
     * @param id 模型ID
     * @return 模型详情
     */
    ModelVO selectModelDetail(@Param("id") Long id);
    
    /**
     * 获取用户的所有模型
     *
     * @param userId 用户ID
     * @param isAdmin 是否为管理员
     * @return 模型列表
     */
    List<ModelVO> selectAllModels(
            @Param("userId") Long userId,
            @Param("isAdmin") boolean isAdmin);
} 