package com.hyq.product.constants;

/**
 * @author nanke
 * @date 2021/9/4 上午5:30
 * 致终于来到这里的勇敢的人:
 * 永远不要放弃！永远不要对自己失望！永远不要逃走辜负了自己。
 * 永远不要哭啼！永远不要说再见！永远不要说慌来伤害目己。
 */
public class RedisPreConstant {

    public static String itemCache = "item:";

    /**
     * 获取RedisKey方法,只允许拥有这一个
     * @param keys
     * @return
     */
    public static String getKey(String... keys) {
        return String.join(":", keys);
    }

}
