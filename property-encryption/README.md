# Spring Boot Property Encryption 属性加密

在实际项目开发过程中，我们的应用程序会有多个配置文件（例如`properties`或者`yml`文件等），我们通常会对配置文件中的敏感信息(比如数据库连接密码、与第三方的通信密钥等)进行处理以防止敏感信息外泄。

传统方式是将敏感信息加入到环境变量或启动参数中，在配置文件中改为使用占位符`${}`代替。传统方式的痛点是启动时要带上大量的参数，当这些信息发生变更时，会使运维难度会大大提升。

比较优雅加密方案是使用 **Jasypt**。`Jasypt` 是一个简单易用的加解密 `Java` 库，提供了自动化配置库 [jasypt-spring-boot](https://github.com/ulisesbocchio/jasypt-spring-boot) ，使用起来非常简单。

### 引入依赖

在文件中 `pom.xml` 添加 `jasypt-spring-boot-starter` 和 `jasypt-maven-plugin`依赖

```xml 
<!-- jasypt 库 -->
<dependency>
  <groupId>com.github.ulisesbocchio</groupId>
  <artifactId>jasypt-spring-boot-starter</artifactId>
  <version>3.0.3</version>
</dependency>

<build>
  <plugins>
    <!-- jasypt 插件 -->
    <plugin>
      <groupId>com.github.ulisesbocchio</groupId>
      <artifactId>jasypt-maven-plugin</artifactId>
      <version>3.0.3</version>
    </plugin>
  </plugins>
</build>
```

### 常用命令

#### 值加密

```shell 
mvn jasypt:encrypt-value\
    -Djasypt.encryptor.password="mypassword"\
    -Djasypt.plugin.value="123456789"
# -> ENC(kdHEEhAcpdP9x3TrCzzy4fbK6J5Ubuj4xZzwVOkbUZ0iZIR7t3/nV96IVjSS+u9f)
```
参数：

- `jasypt.encryptor.password` 是用于加解密的密码，
- `jasypt.plugin.value` 待加密的字符串

注意：Jasypt 默认采用`PBEWITHHMACSHA512ANDAES_256`算法，**每次加密得到的密文是不相同的**，但都可以正常解密。

#### 值解密

```shell 
mvn jasypt:decrypt-value\
    -Djasypt.encryptor.password="mypassword"\
    -Djasypt.plugin.value="ENC(kdHEEhAcpdP9x3TrCzzy4fbK6J5Ubuj4xZzwVOkbUZ0iZIR7t3/nV96IVjSS+u9f)"
# -> 123456789
```

#### 属性文件加密

```shell
mvn jasypt:encrypt\
  -Djasypt.plugin.path="file:src/main/resources/application.yml"\
  -Djasypt.encryptor.password="mypassword"
```

加密前我们要把需要加密的值前后加上`DEC(`和`)`。像这样：

```yaml
# application.yml 文件示例
sensitive:
  password: DEC(secret value)  # -> password: ENC(encrypted)
regular:
  property: example
```

#### 属性文件解密

```shell 
mvn jasypt:decrypt\
  -Djasypt.plugin.path="file:src/main/resources/application.yml"\
  -Djasypt.encryptor.password="mypassword"
```
注意：属性文件解密命令只是将解密后的文件打印到控制台上，并不覆盖属性文件。

#### 重新加密：密码变更

```shell 
mvn jasypt:reencrypt -Djasypt.plugin.old.password=OLD -Djasypt.encryptor.password=NEW\
  -Djasypt.plugin.path="file:src/main/resources/application.yml"
```

#### 重新加密：配置变更

使用旧的配置解密后，再使用新的配置加密

```bash
mvn jasypt:upgrade -Djasypt.encryptor.password=EXAMPLE\
  -Djasypt.plugin.path="file:src/main/resources/application.yml"
```

### 源码解析 jasypt-spring-boot 自动解密流程

Jasypt 向`Spring Bean`容器中注册了BeanFactory后置处理器`EnableEncryptablePropertiesBeanFactoryPostProcessor` 。

```java
package com.ulisesbocchio.jasyptspringboot.configuration;
public class EnableEncryptablePropertiesBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {
  ...
  private final ConfigurableEnvironment environment;
  private final EncryptablePropertySourceConverter converter;
  ...
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        ...
        MutablePropertySources propSources = this.environment.getPropertySources();
        this.converter.convertPropertySources(propSources);//使用convert转换(or 解密)原始属性
  }
  ...
}   
    
```

`BeanFactoryPostProcessor`的作用是在BeanFactory实例化后、Bean初始化前调用`postProcessBeanFactory`对配置元数据进行处理。

```java
package com.ulisesbocchio.jasyptspringboot;
public class EncryptablePropertySourceConverter {
	...
	private final EncryptablePropertyResolver propertyResolver;
	
  private <T> PropertySource<T> convertPropertySource(PropertySource<T> propertySource) {
        return this.interceptionMode == InterceptionMode.PROXY ? this.proxyPropertySource(propertySource) : this.instantiatePropertySource(propertySource); // 两个方法中使用了propertyResolver来处理propertySource
  }
	...
}
```

解密的关键代码在`resolvePropertyValue`方法中

```java
package com.ulisesbocchio.jasyptspringboot.resolver;
public class DefaultPropertyResolver implements EncryptablePropertyResolver {
	  ...
	  public String resolvePropertyValue(String value) {
	  		...
        // 这里是属性解密处理过程
	  }
}
```

### 在项目中的应用

在项目中应由专人负责加密工作（略称：加密者），不同的运行环境应使用各自的加密密码，加密密码不能保存在源码仓库中，应该单独保存。

### 体会

1. 此方案虽然在环境变量或启动参数中配置大量的信息，但不可避免的是加密密码仍需要配置在环境变量或启动参数中，好在只需要配置一次。
2. 即便各运行环境使用的属性值是相同的，也必须分别加密。