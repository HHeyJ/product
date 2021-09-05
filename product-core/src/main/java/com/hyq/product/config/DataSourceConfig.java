package com.hyq.product.config;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;

import static org.springframework.transaction.TransactionDefinition.ISOLATION_DEFAULT;

/**
 * @author nanke
 * @date 2020/11/16 7:05 上午
 * 致终于来到这里的勇敢的人:
 * 永远不要放弃！永远不要对自己失望！永远不要逃走辜负了自己。
 * 永远不要哭啼！永远不要说再见！永远不要说慌来伤害目己。
 */
@Slf4j
@Configuration
@MapperScan(basePackages = {"com.hyq.product.dao"}, sqlSessionFactoryRef = "sqlSessionFactory")
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;
    @Value("${spring.datasource.username}")
    private String userName;
    @Value("${spring.datasource.password}")
    private String passWord;

    @Bean(name = "productDataSource", initMethod = "init")
    public DruidDataSource dataSource() {
        log.info("-------------------- dataSource init ---------------------");
        try (DruidDataSource datasource = new DruidDataSource()) {
            datasource.setUrl(dbUrl);
            datasource.setUsername(userName);
            datasource.setPassword(passWord);
            datasource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            commonConfig(datasource);
            return datasource;
        }
    }


    private void commonConfig(DruidDataSource datasource)  {
        //配置连接池的初始化大小，最大值，最小值
        datasource.setInitialSize(5);
        datasource.setMaxActive(30);
        datasource.setMinIdle(5);
        //配置获取连接等待超时的时间
        datasource.setMaxWait(60000);
        //配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
        datasource.setTimeBetweenEvictionRunsMillis(60000);
        //配置一个连接在池中最小生存的时间，单位是毫秒
        datasource.setMinEvictableIdleTimeMillis(300000);

        //用来检测连接是否有效的sql，要求是一个查询语句，常用select 'x'。如果validationQuery为null，testOnBorrow、testOnReturn、testWhileIdle都不会起作用。
        datasource.setValidationQuery("SELECT 'x'");
        //申请连接时执行validationQuery检测连接是否有效，做了这个配置会降低性能
        datasource.setTestOnBorrow(false);
        //建议配置为true，不影响性能，并且保证安全性。申请连接的时候检测，如果空闲时间大于timeBetweenEvictionRunsMillis，执行validationQuery检测连接是否有效。
        datasource.setTestWhileIdle(true);
        //归还连接时执行validationQuery检测连接是否有效，做了这个配置会降低性能。
        datasource.setTestOnReturn(false);
        //是否缓存preparedStatement，也就是PSCache。PSCache对支持游标的数据库性能提升巨大，比如说oracle。在mysql下建议关闭
        datasource.setPoolPreparedStatements(false);
        //要启用PSCache，必须配置大于0，当大于0时，poolPreparedStatements自动触发修改为true。在Druid中，不会存在Oracle下PSCache占用内存过多的问题，可以把这个数值配置大一些，比如说100
        datasource.setMaxPoolPreparedStatementPerConnectionSize(-1);
        //慢sql的记录
        try {
            datasource.setFilters("stat");
        } catch (SQLException e) {
            log.error("druid configuration initialization filter:",e);
        }
        StatFilter statFilter = new StatFilter();
        statFilter.setSlowSqlMillis(50);
        statFilter.setLogSlowSql(true);
        datasource.setProxyFilters(Collections.singletonList(statFilter));
    }

    /*********************
     *                   *
     *      事务源        *
     *                   *
     *********************/

    @Bean(name = "productTransactionTemplate")
    public TransactionTemplate productTransactionTemplate(@Qualifier("productDataSource") DruidDataSource dataSource) {

        DataSourceTransactionManager transcationManager = new DataSourceTransactionManager(dataSource);
        TransactionTemplate transactionTemplate = new TransactionTemplate();
        transactionTemplate.setIsolationLevel(ISOLATION_DEFAULT);
        transactionTemplate.setTimeout(10);
        transactionTemplate.setTransactionManager(transcationManager);
        return transactionTemplate;
    }

    /*********************
     *                   *
     *    sqlSession     *
     *                   *
     *********************/

    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory mybatisSqlSessionFactory(@Qualifier("productDataSource") DataSource dataSource)
            throws Exception {
        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setConfigLocation(new PathMatchingResourcePatternResolver().getResource("classpath:mybatis-config.xml"));

        org.springframework.core.io.Resource[] localDaoResources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/**/*DAO.xml");

        sessionFactory.setMapperLocations(localDaoResources);
        return sessionFactory.getObject();
    }
}