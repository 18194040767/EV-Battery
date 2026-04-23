package com.evbattery.modules.trade.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evbattery.modules.trade.entity.Order;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface OrderMapper extends BaseMapper<Order> {}
