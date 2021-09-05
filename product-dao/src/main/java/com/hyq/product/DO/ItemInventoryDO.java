package com.hyq.product.DO;

import lombok.Data;

import java.io.Serializable;

@Data
public class ItemInventoryDO implements Serializable {

    /**
     * 商品id
     */
    private Long itemId;
    /**
     * 库存
     */
    private Integer inventory;
    /**
     * 版本号
     */
    private Integer version;

}

