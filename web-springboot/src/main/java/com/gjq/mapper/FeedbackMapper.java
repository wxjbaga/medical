package com.gjq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gjq.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评估反馈Mapper接口
 */
@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
} 