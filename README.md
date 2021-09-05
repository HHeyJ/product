环境
1、jdk 1.8+
2、MySQL
3、Redis

高并发场景库存读写解决方案Demo
1、MySQL表结构
    ./sqltab/item_inventory.sql
2、初始化数据
    com.hyq.product.controller.InitDataController.initInventoryData
3、测试类
    com.hyq.product.controller.InventoryController
4、逻辑类
    com.hyq.product.ConcurrentService