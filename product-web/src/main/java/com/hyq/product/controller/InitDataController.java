package com.hyq.product.controller;

import com.hyq.product.DO.ItemInventoryDO;
import com.hyq.product.dao.ItemInventoryDAO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author nanke
 * @date 2021/9/4 上午8:41
 * 致终于来到这里的勇敢的人:
 * 永远不要放弃！永远不要对自己失望！永远不要逃走辜负了自己。
 * 永远不要哭啼！永远不要说再见！永远不要说慌来伤害目己。
 */
@RestController
public class InitDataController {

    @Resource
    private ItemInventoryDAO itemInventoryDAO;

    @RequestMapping("/initInventoryData")
    public String initInventoryData(@RequestParam("num") Long num) {

        Random random = new Random();

        for (int i = 0; i < num / 1000; i++) {
            List<ItemInventoryDO> list = new ArrayList<>();
            for (int j = 0; j < 1000; j++) {
                ItemInventoryDO insertDO = new ItemInventoryDO();
                insertDO.setInventory(random.nextInt(500));
                list.add(insertDO);
            }
            itemInventoryDAO.batchInsert(list);
        }
        return "商品库存表已初始化" + num + "条数据";
    }












}
