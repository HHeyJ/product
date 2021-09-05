package com.hyq.product.dao;

import com.hyq.product.DO.ItemInventoryDO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemInventoryDAO {

    /**
     * 插入
     * @param insertDO
     */
    public void insert(ItemInventoryDO insertDO);

    /**
     * 批量插入
     * @param list
     */
    public void batchInsert(List<ItemInventoryDO> list);

    /**
     * 更新
     * @param updateDO
     * @return 影响行数
     */
    public Long reduceInventory(ItemInventoryDO updateDO);

    /**
     *
     * @param itemId
     * @return
     */
    public ItemInventoryDO selectById(Long itemId);

}
