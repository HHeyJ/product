 环境  | 版本  
 ---- | -----  
 JDK | 1.8.0_271  
 MySQL | 8.0.26  
 Redis | 6.2.5  

## 1、高并发场景库存读写解决方案Demo
* MySQL表结构  
./sqltab/item_inventory.sql  
* 初始化数据  
com.hyq.product.controller.InitDataController.initInventoryData  
* 测试类  
com.hyq.product.controller.InventoryController  
* 逻辑类  
com.hyq.product.ConcurrentService  