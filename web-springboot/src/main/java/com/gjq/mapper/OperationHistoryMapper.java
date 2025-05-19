package com.gjq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gjq.entity.OperationHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作历史Mapper接口
 */
@Mapper
public interface OperationHistoryMapper extends BaseMapper<OperationHistory> {
} 