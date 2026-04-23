package com.evbattery.modules.battery.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evbattery.common.result.Result;
import com.evbattery.modules.battery.entity.Tag;
import com.evbattery.modules.battery.mapper.TagMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tag")
public class TagController {

    @Resource
    private TagMapper tagMapper;

    @GetMapping("/list")
    public Result<List<Tag>> list() {
        return Result.success(tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getName)));
    }

    @PostMapping
    public Result<Tag> create(@RequestBody Map<String, String> body) {
        if (body == null || !StringUtils.hasText(body.get("name"))) {
            return Result.fail(400, "标签名称不能为空");
        }
        String name = body.get("name");
        Tag exists = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name.trim()));
        if (exists != null) {
            return Result.success("标签已存在", exists);
        }
        Tag tag = new Tag();
        tag.setName(name.trim());
        String color = (body.get("color") != null && StringUtils.hasText(body.get("color"))) ? body.get("color").trim() : "#409EFF";
        tag.setColor(color);
        tagMapper.insert(tag);
        return Result.success("创建成功", tag);
    }
}
