package com.hyq.product.controller;

import com.hyq.product.ConcurrentService;
import com.hyq.product.DO.ItemInventoryDO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author nanke
 * @date 2021/9/4 上午5:58
 * 致终于来到这里的勇敢的人:
 * 永远不要放弃！永远不要对自己失望！永远不要逃走辜负了自己。
 * 永远不要哭啼！永远不要说再见！永远不要说慌来伤害目己。
 */
@RestController
public class InventoryController {

    @Resource
    private ConcurrentService concurrentService;

    /**
     * 库存并发读
     *
     * 问题
     * 1、缓存与DB数据不一致
     * 思路
     * 1、读DB与写缓存采用分布式锁保证数据一致性
     * 2、采用自旋,当缓存数据未命中时争抢分布式锁(阻塞时间自己取舍),当读写操作成功时不释放锁,读写发生异常时才释放锁
     *
     * @param itemId 商品id
     * @param threadNum 模拟并发数
     * @return 测试结果
     * @throws InterruptedException
     */
    @RequestMapping("/concurrentRead")
    public String concurrentRead(@RequestParam("itemId") Long itemId, @RequestParam("threadNum") Integer threadNum) throws InterruptedException {

        CountDownLatch startFlag = new CountDownLatch(1);
        CountDownLatch endFlag = new CountDownLatch(threadNum);
        // 准备threadNum个线程
        List<Thread> threadList = new ArrayList<>();
        for (int i = 0; i < threadNum; i++) {
            Thread thread = new Thread(new ReadThread(itemId,concurrentService,startFlag,endFlag));
            thread.setName("库存模拟并发读线程-" + i);
            threadList.add(thread);
        }

        // threadNum个线程启动
        threadList.forEach(Thread::start);
        // 开始时间
        long beginTime = System.currentTimeMillis();
        // 所有线程启动
        startFlag.countDown();
        // 是否结束
        endFlag.await();
        // 结束时间
        long endTime = System.currentTimeMillis();
        return "并发完成,请查看日志,总线程数:" + threadNum + ",总耗时:" + (endTime - beginTime) + "ms";
    }

    /**
     * 库存并发写
     *
     * 问题
     * 1、超卖
     * 2、IO吞吐量
     * 思路
     * 1、该方法使用乐观锁解决超卖问题
     * 2、聚合请求
     *
     * @param itemId 商品id
     * @param threadNum 模拟并发数
     * @return 测试结果
     * @throws Exception
     */
    @RequestMapping("/concurrentWrite")
    public String concurrentWrite(@RequestParam("itemId") Long itemId, @RequestParam("threadNum") Integer threadNum) throws Exception {
        CountDownLatch startFlag = new CountDownLatch(1);
        CountDownLatch endFlag = new CountDownLatch(threadNum);
        // 扣减对象
        ItemInventoryDO updateDO = new ItemInventoryDO();
        updateDO.setItemId(itemId);
        updateDO.setInventory(1);
        // 准备threadNum个线程
        List<Thread> threadList = new ArrayList<>();
        for (int i = 0; i < threadNum; i++) {
            Thread thread = new Thread(new WriteThread(concurrentService,startFlag,endFlag,updateDO));
            thread.setName("库存模拟并发写线程-" + i);
            threadList.add(thread);
        }

        // threadNum个线程启动
        threadList.forEach(Thread::start);
        // 开始时间
        long beginTime = System.currentTimeMillis();
        // 所有线程启动
        startFlag.countDown();
        // 是否结束
        endFlag.await();
        // 结束时间
        long endTime = System.currentTimeMillis();
        return "并发完成,请查看日志,总线程数:" + threadNum + ",总耗时:" + (endTime - beginTime) + "ms";
    }




    @Slf4j
    @AllArgsConstructor
    private static class ReadThread implements Runnable {

        private Long itemId;
        private ConcurrentService concurrentService;
        private CountDownLatch startFlag;
        private CountDownLatch endFlag;

        @Override
        public void run() {
            try {
                // 等待并发
                startFlag.await();
                concurrentService.read(itemId);
            } catch (Exception e) {

            } finally {
                // 结束
                endFlag.countDown();
            }
        }
    }

    @Slf4j
    @AllArgsConstructor
    private static class WriteThread implements Runnable {

        private ConcurrentService concurrentService;
        private CountDownLatch startFlag;
        private CountDownLatch endFlag;
        private ItemInventoryDO updateDO;

        @Override
        public void run() {
            try {
                // 等待并发
                startFlag.await();
                concurrentService.writeA(updateDO);
            } catch (Exception e) {

            } finally {
                // 结束
                endFlag.countDown();
            }
        }
    }

}
