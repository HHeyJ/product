package com.hyq.product;

import com.hyq.product.DO.ItemInventoryDO;
import com.hyq.product.constants.RedisPreConstant;
import com.hyq.product.dao.ItemInventoryDAO;
import com.hyq.product.enums.EnumRedisLock;
import com.hyq.product.redis.RedisDistLock;
import com.hyq.product.redis.RedisService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author nanke
 * @date 2021/9/4 上午5:59
 * 致终于来到这里的勇敢的人:
 * 永远不要放弃！永远不要对自己失望！永远不要逃走辜负了自己。
 * 永远不要哭啼！永远不要说再见！永远不要说慌来伤害目己。
 */
@Slf4j
@Service
public class ConcurrentService {

    @Resource
    private RedisService redisService;
    @Resource
    private RedisDistLock redisDistLock;
    @Resource
    private ItemInventoryDAO itemInventoryDAO;

    /**
     * 并发读场景,如何保证缓存,db一致性
     */
    public Long read(Long itemId) {
        String cacheKey = RedisPreConstant.getKey(RedisPreConstant.itemCache, itemId.toString());
        // 自旋直到缓存中读取到数据或者抢占到锁后从db中读取
        for (;;) {
            // 并发缓存中读,读到直接返回
            Long inventory = redisService.get(cacheKey);
            if (inventory == null) {
                // 分布式锁,阻塞20ms后从db读,或者等待自旋从缓存中读
                boolean tryLock = redisDistLock.tryLock(EnumRedisLock.INVENTORY_READ, itemId.toString());
                if (tryLock) {
                    // 同步读取
                    try {
                        return readAndWriteCache(itemId);
                    } catch (Exception e) {
                        // 未发生异常不释放锁
                        redisDistLock.unlock(EnumRedisLock.INVENTORY_READ, itemId.toString());
                    }
                }
            } else {
                log.info(Thread.currentThread().getName() + "命中缓存");
                return inventory;
            }
        }
    }

    /**
     * 并发度场景,采用乐观锁解决超卖问题
     */
    public void writeA(ItemInventoryDO updateDO) {

        long beginTime = System.currentTimeMillis();
        Long aLong = itemInventoryDAO.reduceInventory(updateDO);
        long endTime = System.currentTimeMillis();

        if (aLong != 1) {
            log.info(Thread.currentThread().getName() + "扣减库存失败," + "I/O耗时:" + (endTime - beginTime));
        } else {
            log.info(Thread.currentThread().getName() + "扣减库存成功," + "I/O耗时:" + (endTime - beginTime));
        }
    }

    /**
     * 超高并发下,采用时间窗口采集500ms数据后聚合请求,减少IO
     */
    // 阻塞队列,存储时间窗口请求内容,并发安全
    volatile LinkedBlockingQueue<Request> queue = new LinkedBlockingQueue<>();
    // 初始化聚合定时任务
    @PostConstruct
    public void mergeJob() {
        // 一个延时队列
        ScheduledExecutorService scheduledService = Executors.newScheduledThreadPool(1);
        scheduledService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                // 阻塞队列为空,无需聚合
                if (queue.isEmpty()) {
                    return ;
                }
                List<Request> requestList = new ArrayList<>();
                for (int i = 0; i < queue.size(); i++) {
                    Request peek = queue.poll();
                    requestList.add(peek);
                }
                Map<Long, List<Request>> collect = requestList.stream().filter(Objects::nonNull).collect(Collectors.groupingBy(Request::getItemId));
                for (Map.Entry<Long,List<Request>> entry : collect.entrySet()) {
                    List<Request> value = entry.getValue();
                    // 1、根据单件商品进行聚合,先直接扣减成功则全部成功
                    int sum = value.stream().mapToInt(Request::getInventory).sum();
                    ItemInventoryDO updateDO = new ItemInventoryDO();
                    updateDO.setItemId(entry.getKey());
                    updateDO.setInventory(sum);
                    Long aLong = itemInventoryDAO.reduceInventory(updateDO);
                    if (aLong != 1) {
                        // 2、排序后获取到当前库存找到不超过的操作进行一次扣减,成功则返回成功部分
                        Long readInventory = read(entry.getKey());
                        if (readInventory.equals(0L)) {
                            value.forEach(e -> e.getFuture().complete(false));
                        } else {
                            List<Request> executorList = new ArrayList<>();
                            sum = 0;
                            for (Request request : value) {
                                int temp = sum + request.getInventory();
                                if (temp <= readInventory) {
                                    sum = temp;
                                    executorList.add(request);
                                } else {
                                    // 根据当前库存数量未选中的请求直接失败
                                    request.getFuture().complete(false);
                                }
                            }
                            if (sum > 0) {
                                updateDO.setInventory(sum);
                                aLong = itemInventoryDAO.reduceInventory(updateDO);
                                // 3、最坏情况,串行单个执行
                                if (aLong != 1) {
                                    executorList.forEach(e -> {
                                        updateDO.setInventory(e.getInventory());
                                        e.getFuture().complete(itemInventoryDAO.reduceInventory(updateDO).equals(1L));
                                    });
                                } else {
                                    executorList.forEach(e -> e.getFuture().complete(true));
                                }
                            }
                        }
                    } else {
                        value.forEach(e -> e.getFuture().complete(true));
                    }
                }
            }
        },0,500,TimeUnit.MILLISECONDS);
    }

    public void writeB(ItemInventoryDO updateDO) {

        Request request = new Request();
        request.setItemId(updateDO.getItemId());
        request.setInventory(updateDO.getInventory());

        queue.add(request);
        try {
            long beginTime = System.currentTimeMillis();
            // 阻塞等待结果
            Boolean result = request.getFuture().get();
            long endTime = System.currentTimeMillis();
            if (result) {
                log.info(Thread.currentThread().getName() + "扣减库存成功," + "I/O耗时:" + (endTime - beginTime));
            } else {
                log.info(Thread.currentThread().getName() + "扣减库存失败," + "I/O耗时:" + (endTime - beginTime));
            }
        } catch (Exception e) {
            log.info(Thread.currentThread().getName() + "扣减库存发生异常",e.getMessage());
        }
    }

    /**
     * 聚合请求对象,内置CompletableFuture获取聚合线程所给到的结果
     */
    @Data
    private static class Request {

        private Long itemId;

        private Integer inventory;

        private CompletableFuture<Boolean> future = new CompletableFuture<>();

    }







    /**
     * 读取Db并且写入缓存
     */
    private Long readAndWriteCache(Long itemId) {
        if (itemId == null) {
            return 0L;
        }

        long beginTime = System.currentTimeMillis();

        ItemInventoryDO itemInventoryDO = itemInventoryDAO.selectById(itemId);
        Long inventory = itemInventoryDO == null ? 0L : itemInventoryDO.getInventory();
        String cacheKey = RedisPreConstant.getKey(RedisPreConstant.itemCache, itemId.toString());
        redisService.set(cacheKey,inventory,10);

        long endTime = System.currentTimeMillis();

        log.info(Thread.currentThread().getName() + "命中DB,耗时:" + (endTime - beginTime) + "ms");
        return inventory;
    }


}
