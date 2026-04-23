package com.evbattery.modules.battery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evbattery.modules.battery.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
}
