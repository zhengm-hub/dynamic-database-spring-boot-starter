### dynamic db

> 参考

[baomidou/dynamic-datasource-spring-boot-starter](https://github.com/baomidou/dynamic-datasource-spring-boot-starter)

> 引入依赖

```javascript
<dependency>
    <groupId>com.dynamic.ds</groupId>
    <artifactId>dynamic-database-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

> 添加配置

```javascript
custom:
    dynamic:
      datasource:
        master:
          # 这里设置连接池信息
          username: root
          password: password
          url: jdbc:mysql://192.168.1.171:3309/db1?useSSL=false&characterEncoding=utf8
          driver-class-name: com.mysql.cj.jdbc.Driver
        slave_1:
          username: root
          password: 
          url: jdbc:mysql://192.168.1.171:3309/db2?useSSL=false&characterEncoding=utf8
          driver-class-name: com.mysql.cj.jdbc.Driver
        slave_2:
          username: root
          password: password
          url: jdbc:mysql://192.168.1.171:3309/db3?useSSL=false&characterEncoding=utf8
          driver-class-name: com.mysql.cj.jdbc.Driver
```


> 事务

```javascript
// 1 单数据源事务
@DS("slave_1")
@Transactional(rollbackFor = Throwable.class)
public void addUserSlave1Transactional(User user1, User user2) {
    logger.info("will add from slave1 ....");
    dynamicDsMapper.insert(user1);
    int i = 1/0;
    dynamicDsMapper.insert(user2);
}

// 2 多数据源事务

// 排除依赖
<dependency>
    <groupId>com.dynamic.ds</groupId>
    <artifactId>dynamic-database-spring-boot-starter</artifactId>
    <version>1.0.0</version>
    <exclusions>
        <exclusion>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
        </exclusion>
    </exclusions>
</dependency>
// 加入jta-atomikos
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jta-atomikos</artifactId>
</dependency>
// 代码片段
/**
 * 分布式跨库事务需要用编程式事务
 * spring默认的注解事务不支持跨库事务
 */
@Autowired
private JtaTransactionManager transactionManager;

public void addTx() throws Exception {
    TransactionManager tx = this.transactionManager.getTransactionManager();
    tx.begin();
    try {
         // 
         // 调用本类中方法，需要用代理调用，因为Spring AOP（@DS）采用了动态代理实现，在Spring容器中的bean（也就是目标对象）会被代理对象代替
         // 直接用this调用，代理切换不过来，无法切入，需要手动切换代理对象调用
        ((UnionService)(AopContext.currentProxy())).addUser();
        ((UnionService)(AopContext.currentProxy())).addSalary();
        int i = 1 / 0;
        tx.commit();
    } catch (Exception e) {
        tx.rollback();
        throw e;
    }
}

@DS("slave_1")
public void addUser() {
    User user = new User("didi", 5);
    userMapper.insert(user);
}

@DS("slave_2")
public void addSalary() {
    Salary salary = new Salary(1,5);
    salaryMapper.insert(salary);
}
```
