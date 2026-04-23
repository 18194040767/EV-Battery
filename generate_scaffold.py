import os
from textwrap import dedent

ROOT = r"d:\HuaweiMoveData\Users\hbj\Desktop\EV-Battery"


def write(rel_path: str, content: str):
    full = os.path.join(ROOT, rel_path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8", newline="\n") as f:
        f.write(dedent(content).strip() + "\n")


backend_base = "ev-battery-platform-backend"
frontend_base = "ev-battery-platform-frontend"

write(
    f"{backend_base}/pom.xml",
    """
    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>
        <parent>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-parent</artifactId>
            <version>2.7.18</version>
            <relativePath/>
        </parent>
        <groupId>com.evbattery</groupId>
        <artifactId>ev-battery-platform-backend</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <name>ev-battery-platform-backend</name>
        <properties>
            <java.version>1.8</java.version>
            <mybatis-plus.version>3.5.5</mybatis-plus.version>
            <knife4j.version>4.4.0</knife4j.version>
            <jjwt.version>0.9.1</jjwt.version>
        </properties>
        <dependencies>
            <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
            <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
            <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-aop</artifactId></dependency>
            <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
            <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-redis</artifactId></dependency>
            <dependency><groupId>mysql</groupId><artifactId>mysql-connector-java</artifactId><scope>runtime</scope></dependency>
            <dependency><groupId>com.baomidou</groupId><artifactId>mybatis-plus-boot-starter</artifactId><version>${mybatis-plus.version}</version></dependency>
            <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt</artifactId><version>${jjwt.version}</version></dependency>
            <dependency><groupId>com.github.xiaoymin</groupId><artifactId>knife4j-spring-boot-starter</artifactId><version>${knife4j.version}</version></dependency>
            <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
            <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
        </dependencies>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                </plugin>
            </plugins>
        </build>
    </project>
    """,
)

write(
    f"{backend_base}/src/main/java/com/evbattery/EvBatteryPlatformApplication.java",
    """
    package com.evbattery;

    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;

    @SpringBootApplication
    public class EvBatteryPlatformApplication {
        public static void main(String[] args) {
            SpringApplication.run(EvBatteryPlatformApplication.class, args);
        }
    }
    """,
)

write(
    f"{backend_base}/src/main/java/com/evbattery/common/result/Result.java",
    """
    package com.evbattery.common.result;

    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class Result<T> {
        private Integer code;
        private String message;
        private T data;

        public static <T> Result<T> success(T data) {
            return new Result<>(200, "success", data);
        }

        public static <T> Result<T> success(String message, T data) {
            return new Result<>(200, message, data);
        }

        public static <T> Result<T> fail(Integer code, String message) {
            return new Result<>(code, message, null);
        }
    }
    """,
)

write(
    f"{backend_base}/src/main/java/com/evbattery/common/exception/BusinessException.java",
    """
    package com.evbattery.common.exception;

    public class BusinessException extends RuntimeException {
        private final Integer code;

        public BusinessException(Integer code, String message) {
            super(message);
            this.code = code;
        }

        public Integer getCode() {
            return code;
        }
    }
    """,
)

write(
    f"{backend_base}/src/main/java/com/evbattery/common/exception/GlobalExceptionHandler.java",
    """
    package com.evbattery.common.exception;

    import com.evbattery.common.result.Result;
    import org.springframework.web.bind.MethodArgumentNotValidException;
    import org.springframework.web.bind.annotation.ExceptionHandler;
    import org.springframework.web.bind.annotation.RestControllerAdvice;

    @RestControllerAdvice
    public class GlobalExceptionHandler {

        @ExceptionHandler(BusinessException.class)
        public Result<Void> handleBusiness(BusinessException ex) {
            return Result.fail(ex.getCode(), ex.getMessage());
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public Result<Void> handleValid(MethodArgumentNotValidException ex) {
            return Result.fail(400, ex.getBindingResult().getFieldError().getDefaultMessage());
        }

        @ExceptionHandler(Exception.class)
        public Result<Void> handleException(Exception ex) {
            return Result.fail(500, ex.getMessage());
        }
    }
    """,
)

write(
    f"{backend_base}/src/main/java/com/evbattery/common/BaseEntity.java",
    """
    package com.evbattery.common;

    import com.baomidou.mybatisplus.annotation.FieldFill;
    import com.baomidou.mybatisplus.annotation.TableField;
    import lombok.Data;
    import java.time.LocalDateTime;

    @Data
    public class BaseEntity {
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }
    """,
)

write(
    f"{backend_base}/src/main/java/com/evbattery/config/MybatisPlusConfig.java",
    """
    package com.evbattery.config;

    import com.baomidou.mybatisplus.annotation.DbType;
    import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
    import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;

    @Configuration
    public class MybatisPlusConfig {
        @Bean
        public MybatisPlusInterceptor mybatisPlusInterceptor() {
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
            return interceptor;
        }
    }
    """,
)

write(
    f"{backend_base}/src/main/java/com/evbattery/config/Knife4jConfig.java",
    """
    package com.evbattery.config;

    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import springfox.documentation.builders.PathSelectors;
    import springfox.documentation.builders.RequestHandlerSelectors;
    import springfox.documentation.spi.DocumentationType;
    import springfox.documentation.spring.web.plugins.Docket;

    @Configuration
    public class Knife4jConfig {
        @Bean
        public Docket api() {
            return new Docket(DocumentationType.OAS_30)
                    .select()
                    .apis(RequestHandlerSelectors.basePackage("com.evbattery"))
                    .paths(PathSelectors.any())
                    .build();
        }
    }
    """,
)

write(
    f"{backend_base}/src/main/java/com/evbattery/security/JwtTokenUtil.java",
    """
    package com.evbattery.security;

    import io.jsonwebtoken.Claims;
    import io.jsonwebtoken.Jwts;
    import io.jsonwebtoken.SignatureAlgorithm;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.stereotype.Component;

    import java.util.Date;
    import java.util.HashMap;
    import java.util.Map;

    @Component
    public class JwtTokenUtil {
        @Value("${security.jwt.secret}")
        private String secret;

        @Value("${security.jwt.expire-ms}")
        private Long expireMs;

        public String generateToken(Long userId, String username) {
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId);
            claims.put("username", username);
            Date now = new Date();
            Date expiry = new Date(now.getTime() + expireMs);
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(username)
                    .setIssuedAt(now)
                    .setExpiration(expiry)
                    .signWith(SignatureAlgorithm.HS512, secret)
                    .compact();
        }

        public Claims parse(String token) {
            return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        }
    }
    """,
)

write(
    f"{backend_base}/src/main/java/com/evbattery/security/JwtAuthInterceptor.java",
    """
    package com.evbattery.security;

    import io.jsonwebtoken.Claims;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.stereotype.Component;
    import org.springframework.web.servlet.HandlerInterceptor;
    import javax.annotation.Resource;
    import javax.servlet.http.HttpServletRequest;
    import javax.servlet.http.HttpServletResponse;

    @Slf4j
    @Component
    public class JwtAuthInterceptor implements HandlerInterceptor {
        @Resource
        private JwtTokenUtil jwtTokenUtil;

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String uri = request.getRequestURI();
            if (uri.startsWith("/api/user/login") || uri.startsWith("/api/user/register")
                    || uri.startsWith("/doc.html") || uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger")) {
                return true;
            }
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                response.setStatus(401);
                return false;
            }
            try {
                String token = auth.substring(7);
                Claims claims = jwtTokenUtil.parse(token);
                request.setAttribute("userId", claims.get("userId"));
                request.setAttribute("username", claims.get("username"));
                return true;
            } catch (Exception e) {
                log.warn("JWT validate failed", e);
                response.setStatus(401);
                return false;
            }
        }
    }
    """,
)

write(
    f"{backend_base}/src/main/java/com/evbattery/config/WebMvcConfig.java",
    """
    package com.evbattery.config;

    import com.evbattery.security.JwtAuthInterceptor;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
    import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
    import javax.annotation.Resource;

    @Configuration
    public class WebMvcConfig implements WebMvcConfigurer {
        @Resource
        private JwtAuthInterceptor jwtAuthInterceptor;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(jwtAuthInterceptor).addPathPatterns("/**");
        }
    }
    """,
)

write(f"{backend_base}/src/main/resources/application.yml", "spring:\n  profiles:\n    active: dev\n")
write(
    f"{backend_base}/src/main/resources/application-dev.yml",
    """
    server:
      port: 8080

    spring:
      datasource:
        url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:ev_battery_platform}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
        username: ${DB_USERNAME:root}
        password: ${DB_PASSWORD:root}
        driver-class-name: com.mysql.cj.jdbc.Driver
      redis:
        host: ${REDIS_HOST:localhost}
        port: ${REDIS_PORT:6379}
        password: ${REDIS_PASSWORD:}

    mybatis-plus:
      mapper-locations: classpath*:/mapper/**/*.xml
      configuration:
        map-underscore-to-camel-case: true

    security:
      jwt:
        secret: ${JWT_SECRET:evBatterySecretKey}
        expire-ms: 86400000

    knife4j:
      enable: true
    """,
)
write(
    f"{backend_base}/src/main/resources/application-prod.yml",
    """
    server:
      port: 8080

    spring:
      datasource:
        url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
        username: ${DB_USERNAME}
        password: ${DB_PASSWORD}
        driver-class-name: com.mysql.cj.jdbc.Driver
      redis:
        host: ${REDIS_HOST}
        port: ${REDIS_PORT}
        password: ${REDIS_PASSWORD}

    mybatis-plus:
      mapper-locations: classpath*:/mapper/**/*.xml

    security:
      jwt:
        secret: ${JWT_SECRET}
        expire-ms: ${JWT_EXPIRE_MS:86400000}

    knife4j:
      enable: false
    """,
)

write(
    f"{backend_base}/Dockerfile",
    """
    FROM eclipse-temurin:8-jre
    WORKDIR /app
    COPY target/ev-battery-platform-backend-1.0.0-SNAPSHOT.jar app.jar
    EXPOSE 8080
    ENTRYPOINT ["java","-jar","/app/app.jar"]
    """,
)

write(
    f"{backend_base}/src/main/resources/db/schema.sql",
    """
    CREATE DATABASE IF NOT EXISTS ev_battery_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    USE ev_battery_platform;

    CREATE TABLE IF NOT EXISTS user (
      id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
      username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
      password VARCHAR(255) NOT NULL COMMENT '密码哈希',
      real_name VARCHAR(100) COMMENT '真实姓名',
      phone VARCHAR(20) COMMENT '手机号',
      email VARCHAR(100) COMMENT '邮箱',
      status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1正常0禁用',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
    """,
)

# append remaining sql
with open(os.path.join(ROOT, backend_base, "src/main/resources/db/schema.sql"), "a", encoding="utf-8", newline="\n") as f:
    f.write(dedent("""
    CREATE TABLE IF NOT EXISTS role (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      role_code VARCHAR(50) NOT NULL UNIQUE,
      role_name VARCHAR(100) NOT NULL,
      description VARCHAR(255),
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

    CREATE TABLE IF NOT EXISTS permission (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      perm_code VARCHAR(100) NOT NULL UNIQUE,
      perm_name VARCHAR(100) NOT NULL,
      perm_type VARCHAR(20) DEFAULT 'API',
      path VARCHAR(200),
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

    CREATE TABLE IF NOT EXISTS user_role (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      user_id BIGINT NOT NULL,
      role_id BIGINT NOT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      UNIQUE KEY uk_user_role (user_id, role_id),
      CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES user(id),
      CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES role(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

    CREATE TABLE IF NOT EXISTS role_permission (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      role_id BIGINT NOT NULL,
      permission_id BIGINT NOT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      UNIQUE KEY uk_role_perm (role_id, permission_id),
      CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES role(id),
      CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permission(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联';

    CREATE TABLE IF NOT EXISTS battery_record (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      battery_code VARCHAR(64) NOT NULL UNIQUE COMMENT '电池唯一编码',
      source_type VARCHAR(30) NOT NULL COMMENT '来源:4S店/换电站',
      bms_raw_file_path VARCHAR(255),
      feature_json JSON,
      audit_status TINYINT DEFAULT 0 COMMENT '审核状态:0待审1通过2驳回',
      created_by BIGINT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电池档案';

    CREATE TABLE IF NOT EXISTS battery_upload_log (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      battery_record_id BIGINT,
      file_name VARCHAR(255),
      upload_status TINYINT DEFAULT 1,
      message VARCHAR(255),
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      CONSTRAINT fk_bul_battery FOREIGN KEY (battery_record_id) REFERENCES battery_record(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电池上传日志';

    CREATE TABLE IF NOT EXISTS health_assessment (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      battery_id BIGINT NOT NULL,
      score DECIMAL(5,2),
      grade VARCHAR(10),
      suggestion VARCHAR(500),
      visualization_json JSON,
      assessor_type VARCHAR(20) DEFAULT 'RULE_ML',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      CONSTRAINT fk_assessment_battery FOREIGN KEY (battery_id) REFERENCES battery_record(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康评估记录';

    CREATE TABLE IF NOT EXISTS product (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      seller_id BIGINT NOT NULL,
      battery_id BIGINT NOT NULL,
      title VARCHAR(150) NOT NULL,
      description TEXT,
      price DECIMAL(12,2) NOT NULL,
      stock INT DEFAULT 1,
      status TINYINT DEFAULT 1,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      CONSTRAINT fk_product_seller FOREIGN KEY (seller_id) REFERENCES user(id),
      CONSTRAINT fk_product_battery FOREIGN KEY (battery_id) REFERENCES battery_record(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电池商品';

    CREATE TABLE IF NOT EXISTS purchase_demand (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      buyer_id BIGINT NOT NULL,
      title VARCHAR(150) NOT NULL,
      requirement TEXT,
      budget_min DECIMAL(12,2),
      budget_max DECIMAL(12,2),
      status TINYINT DEFAULT 1,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      CONSTRAINT fk_demand_buyer FOREIGN KEY (buyer_id) REFERENCES user(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购需求';

    CREATE TABLE IF NOT EXISTS `order` (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      order_no VARCHAR(64) NOT NULL UNIQUE,
      product_id BIGINT NOT NULL,
      buyer_id BIGINT NOT NULL,
      seller_id BIGINT NOT NULL,
      amount DECIMAL(12,2) NOT NULL,
      order_status VARCHAR(30) DEFAULT 'CREATED',
      pay_status VARCHAR(30) DEFAULT 'UNPAID',
      remark VARCHAR(255),
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      CONSTRAINT fk_order_product FOREIGN KEY (product_id) REFERENCES product(id),
      CONSTRAINT fk_order_buyer FOREIGN KEY (buyer_id) REFERENCES user(id),
      CONSTRAINT fk_order_seller FOREIGN KEY (seller_id) REFERENCES user(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易订单';

    CREATE TABLE IF NOT EXISTS contract (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      order_id BIGINT NOT NULL,
      pdf_path VARCHAR(255),
      hash_digest VARCHAR(128),
      notarization_tx_id VARCHAR(128),
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      CONSTRAINT fk_contract_order FOREIGN KEY (order_id) REFERENCES `order`(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子合同存证';

    CREATE TABLE IF NOT EXISTS logistics_info (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      order_id BIGINT NOT NULL,
      company VARCHAR(100),
      tracking_no VARCHAR(100),
      status VARCHAR(50),
      nodes_json JSON,
      hazardous_notice TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      CONSTRAINT fk_logistics_order FOREIGN KEY (order_id) REFERENCES `order`(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流信息';

    CREATE TABLE IF NOT EXISTS credit_score (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      user_id BIGINT NOT NULL UNIQUE,
      score INT DEFAULT 100,
      level VARCHAR(20) DEFAULT 'A',
      updated_reason VARCHAR(255),
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      CONSTRAINT fk_credit_user FOREIGN KEY (user_id) REFERENCES user(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信用分';

    CREATE TABLE IF NOT EXISTS report (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      related_type VARCHAR(20) NOT NULL COMMENT 'BATTERY/ORDER',
      related_id BIGINT NOT NULL,
      version_no VARCHAR(50),
      content LONGTEXT,
      summary VARCHAR(500),
      created_by BIGINT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能报告';
    """).strip() + "\n")

write(
    f"{frontend_base}/package.json",
    """
    {
      "name": "ev-battery-platform-frontend",
      "version": "1.0.0",
      "private": true,
      "type": "module",
      "scripts": {
        "dev": "vite",
        "build": "vite build",
        "preview": "vite preview"
      },
      "dependencies": {
        "axios": "^1.7.2",
        "element-plus": "^2.8.0",
        "pinia": "^2.1.7",
        "vue": "^3.4.29",
        "vue-router": "^4.4.0"
      },
      "devDependencies": {
        "@vitejs/plugin-vue": "^5.0.5",
        "vite": "^5.3.1"
      }
    }
    """,
)

write(f"{frontend_base}/vite.config.js", "import { defineConfig } from 'vite'\nimport vue from '@vitejs/plugin-vue'\nexport default defineConfig({ plugins: [vue()], server: { port: 5173 } })\n")
write(f"{frontend_base}/index.html", '<!doctype html><html lang="zh-CN"><head><meta charset="UTF-8" /><meta name="viewport" content="width=device-width, initial-scale=1.0" /><title>EV Battery Platform</title></head><body><div id="app"></div><script type="module" src="/src/main.js"></script></body></html>\n')
write(f"{frontend_base}/src/main.js", "import { createApp } from 'vue'\nimport { createPinia } from 'pinia'\nimport ElementPlus from 'element-plus'\nimport 'element-plus/dist/index.css'\nimport App from './App.vue'\nimport router from './router'\ncreateApp(App).use(createPinia()).use(router).use(ElementPlus).mount('#app')\n")
write(f"{frontend_base}/src/App.vue", "<template><router-view /></template>\n")
write(
    f"{frontend_base}/src/utils/request.js",
    """
    import axios from 'axios'
    import { useUserStore } from '../store/user'
    import router from '../router'

    const request = axios.create({ baseURL: '/api', timeout: 10000 })
    request.interceptors.request.use((config) => {
      const userStore = useUserStore()
      if (userStore.token) config.headers.Authorization = `Bearer ${userStore.token}`
      return config
    })
    request.interceptors.response.use((res) => res.data, (error) => {
      if (error?.response?.status === 401) {
        const userStore = useUserStore()
        userStore.clearAuth()
        router.push('/login')
      }
      return Promise.reject(error)
    })
    export default request
    """,
)
write(
    f"{frontend_base}/src/store/user.js",
    """
    import { defineStore } from 'pinia'
    export const useUserStore = defineStore('user', {
      state: () => ({ token: localStorage.getItem('token') || '', userInfo: null }),
      actions: {
        setToken(token) { this.token = token; localStorage.setItem('token', token) },
        setUserInfo(userInfo) { this.userInfo = userInfo },
        clearAuth() { this.token = ''; this.userInfo = null; localStorage.removeItem('token') }
      }
    })
    """,
)
write(
    f"{frontend_base}/src/layout/Layout.vue",
    """
    <template>
      <el-container style="height: 100vh">
        <el-aside width="220px">
          <el-menu router default-active="/">
            <el-menu-item index="/">首页</el-menu-item>
            <el-menu-item index="/battery/list">电池档案</el-menu-item>
            <el-menu-item index="/assessment/list">健康评估</el-menu-item>
            <el-menu-item index="/trade/product-list">交易撮合</el-menu-item>
            <el-menu-item index="/contract/list">电子合同</el-menu-item>
            <el-menu-item index="/logistics/list">物流追踪</el-menu-item>
            <el-menu-item index="/admin/dashboard">后台管理</el-menu-item>
            <el-menu-item index="/statistics/overview">统计可视化</el-menu-item>
            <el-menu-item index="/report/list">智能报告</el-menu-item>
          </el-menu>
        </el-aside>
        <el-container>
          <el-header>EV-BatterySecondLife</el-header>
          <el-main><router-view /></el-main>
        </el-container>
      </el-container>
    </template>
    """,
)

# module generation
mods = {
    "user": (["User", "Role", "Permission"], ["LoginDTO", "RegisterDTO", "ChangePasswordDTO"], ["LoginVO", "UserVO"], ["UserMapper", "RoleMapper", "PermissionMapper"]),
    "battery": (["BatteryRecord"], ["BatteryUploadDTO"], ["BatteryVO"], ["BatteryRecordMapper"]),
    "assessment": (["HealthAssessment"], ["AssessmentTriggerDTO"], ["AssessmentReportVO"], ["HealthAssessmentMapper"]),
    "trade": (["Product", "PurchaseDemand", "Order"], ["ProductPublishDTO", "DemandPublishDTO", "OrderCreateDTO", "OrderReviewDTO"], ["ProductVO", "DemandVO", "OrderVO"], ["ProductMapper", "PurchaseDemandMapper", "OrderMapper"]),
    "contract": (["Contract"], ["ContractGenerateDTO"], ["ContractVO"], ["ContractMapper"]),
    "logistics": (["LogisticsInfo"], ["TrackingFillDTO"], ["LogisticsVO"], ["LogisticsInfoMapper"]),
    "admin": ([], ["UserManageDTO", "BatteryAuditDTO"], ["DashboardVO"], []),
    "statistics": ([], [], ["TrendVO", "DistributionVO"], []),
    "report": (["Report"], ["ReportGenerateDTO"], ["ReportVO", "ReportCompareVO"], ["ReportMapper"]),
}
extras = {
    "battery": ["DataCleanService"],
    "assessment": ["RuleScoreService", "MLPredictService"],
    "trade": ["CreditService"],
    "contract": ["HashNotarizationService"],
    "logistics": ["LogisticsQueryService"],
    "report": ["LLMService"],
}
entity_fields = {
    "User": ["Long id", "String username", "String password", "String realName", "String phone", "String email", "Integer status"],
    "Role": ["Long id", "String roleCode", "String roleName", "String description"],
    "Permission": ["Long id", "String permCode", "String permName", "String permType", "String path"],
    "BatteryRecord": ["Long id", "String batteryCode", "String sourceType", "String bmsRawFilePath", "String featureJson", "Integer auditStatus", "Long createdBy"],
    "HealthAssessment": ["Long id", "Long batteryId", "Double score", "String grade", "String suggestion", "String visualizationJson"],
    "Product": ["Long id", "Long sellerId", "Long batteryId", "String title", "String description", "java.math.BigDecimal price", "Integer stock", "Integer status"],
    "PurchaseDemand": ["Long id", "Long buyerId", "String title", "String requirement", "java.math.BigDecimal budgetMin", "java.math.BigDecimal budgetMax", "Integer status"],
    "Order": ["Long id", "String orderNo", "Long productId", "Long buyerId", "Long sellerId", "java.math.BigDecimal amount", "String orderStatus", "String payStatus", "String remark"],
    "Contract": ["Long id", "Long orderId", "String pdfPath", "String hashDigest", "String notarizationTxId"],
    "LogisticsInfo": ["Long id", "Long orderId", "String company", "String trackingNo", "String status", "String nodesJson", "String hazardousNotice"],
    "Report": ["Long id", "String relatedType", "Long relatedId", "String versionNo", "String content", "String summary", "Long createdBy"],
}

def snake(name: str) -> str:
    out = []
    for i, c in enumerate(name):
        if c.isupper() and i > 0:
            out.append("_")
        out.append(c.lower())
    return "".join(out)

for mod, (entities, dtos, vos, mappers) in mods.items():
    pkg = f"com.evbattery.modules.{mod}"
    base = f"{backend_base}/src/main/java/com/evbattery/modules/{mod}"
    for d in ["controller", "service", "service/impl", "mapper", "entity", "dto", "vo"]:
        os.makedirs(os.path.join(ROOT, base, d), exist_ok=True)

    for e in entities:
        fields = "\n".join([f"    private {x};" for x in entity_fields[e]])
        write(
            f"{base}/entity/{e}.java",
            f"""
            package {pkg}.entity;
            import com.baomidou.mybatisplus.annotation.TableName;
            import com.evbattery.common.BaseEntity;
            import lombok.Data;
            import lombok.EqualsAndHashCode;
            @Data
            @EqualsAndHashCode(callSuper = true)
            @TableName("{snake(e)}")
            public class {e} extends BaseEntity {{
            {fields}
            }}
            """,
        )
    for d in dtos:
        write(f"{base}/dto/{d}.java", f"package {pkg}.dto;\nimport lombok.Data;\n@Data\npublic class {d} {{\n    private Long id;\n    private String keyword;\n    private String remark;\n}}\n")
    for v in vos:
        write(f"{base}/vo/{v}.java", f"package {pkg}.vo;\nimport lombok.Data;\n@Data\npublic class {v} {{\n    private Long id;\n    private String message;\n    private Object detail;\n}}\n")
    for m in mappers:
        ent = "User"
        if "Role" in m: ent = "Role"
        elif "Permission" in m: ent = "Permission"
        elif "Battery" in m: ent = "BatteryRecord"
        elif "Health" in m: ent = "HealthAssessment"
        elif "Product" in m: ent = "Product"
        elif "Purchase" in m: ent = "PurchaseDemand"
        elif m == "OrderMapper": ent = "Order"
        elif "Contract" in m: ent = "Contract"
        elif "Logistics" in m: ent = "LogisticsInfo"
        elif "Report" in m: ent = "Report"
        write(f"{base}/mapper/{m}.java", f"package {pkg}.mapper;\nimport com.baomidou.mybatisplus.core.mapper.BaseMapper;\nimport com.evbattery.modules.{mod}.entity.{ent};\nimport org.apache.ibatis.annotations.Mapper;\n@Mapper\npublic interface {m} extends BaseMapper<{ent}> {{}}\n")

    svc = f"{mod[:1].upper()}{mod[1:]}Service"
    write(f"{base}/service/{svc}.java", f"package {pkg}.service;\nimport com.evbattery.common.result.Result;\npublic interface {svc} {{ Result<?> placeholder(); }}\n")
    write(f"{base}/service/impl/{svc}Impl.java", f"package {pkg}.service.impl;\nimport com.evbattery.common.result.Result;\nimport {pkg}.service.{svc};\nimport org.springframework.stereotype.Service;\n@Service\npublic class {svc}Impl implements {svc} {{ @Override public Result<?> placeholder() {{ return Result.success(\"{mod} service placeholder\", null); }} }}\n")
    for ex in extras.get(mod, []):
        write(f"{base}/service/{ex}.java", f"package {pkg}.service;\npublic interface {ex} {{ default Object execute(Object input) {{ // TODO: 调用实际模型/API\n return null; }} }}\n")
        write(f"{base}/service/impl/{ex}Impl.java", f"package {pkg}.service.impl;\nimport {pkg}.service.{ex};\nimport org.springframework.stereotype.Service;\n@Service\npublic class {ex}Impl implements {ex} {{ @Override public Object execute(Object input) {{ // TODO: 调用实际模型/API\n return \"mock\"; }} }}\n")

write(
    f"{backend_base}/src/main/java/com/evbattery/modules/user/controller/UserController.java",
    """
    package com.evbattery.modules.user.controller;
    import com.evbattery.common.result.Result;
    import com.evbattery.modules.user.dto.*;
    import com.evbattery.modules.user.vo.*;
    import com.evbattery.security.JwtTokenUtil;
    import org.springframework.web.bind.annotation.*;
    import javax.annotation.Resource;
    import java.util.Collections;
    @RestController
    @RequestMapping("/api/user")
    public class UserController {
        @Resource
        private JwtTokenUtil jwtTokenUtil;
        @PostMapping("/login")
        public Result<LoginVO> login(@RequestBody LoginDTO dto) {
            LoginVO vo = new LoginVO();
            vo.setMessage("登录成功");
            vo.setDetail(Collections.singletonMap("token", jwtTokenUtil.generateToken(1L, "demo")));
            return Result.success(vo);
        }
        @PostMapping("/register")
        public Result<String> register(@RequestBody RegisterDTO dto) { return Result.success("注册接口预留", null); }
        @GetMapping("/current")
        public Result<UserVO> current() { return Result.success(new UserVO()); }
        @PostMapping("/change-password")
        public Result<String> changePassword(@RequestBody ChangePasswordDTO dto) { return Result.success("修改密码接口预留", null); }
    }
    """,
)

controllers = {
    "battery": """
    package com.evbattery.modules.battery.controller;
    import com.evbattery.common.result.Result;
    import com.evbattery.modules.battery.vo.BatteryVO;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.web.multipart.MultipartFile;
    import java.util.Collections;
    import java.util.List;
    @RestController
    @RequestMapping("/api/battery")
    public class BatteryController {
        @PostMapping("/upload/single")
        public Result<String> uploadSingle(@RequestParam("file") MultipartFile file) { return Result.success("单文件上传预留", null); }
        @PostMapping("/upload/batch")
        public Result<String> uploadBatch(@RequestParam("files") MultipartFile[] files) { return Result.success("批量上传预留", null); }
        @GetMapping("/list")
        public Result<List<BatteryVO>> list() { return Result.success(Collections.emptyList()); }
        @GetMapping("/{id}")
        public Result<BatteryVO> detail(@PathVariable Long id) { return Result.success(new BatteryVO()); }
    }
    """,
    "assessment": """
    package com.evbattery.modules.assessment.controller;
    import com.evbattery.common.result.Result;
    import com.evbattery.modules.assessment.dto.AssessmentTriggerDTO;
    import com.evbattery.modules.assessment.vo.AssessmentReportVO;
    import org.springframework.web.bind.annotation.*;
    import java.util.Collections;
    import java.util.List;
    @RestController
    @RequestMapping("/api/assessment")
    public class AssessmentController {
        @PostMapping("/trigger")
        public Result<String> trigger(@RequestBody AssessmentTriggerDTO dto) { return Result.success("触发评估任务已受理", null); }
        @GetMapping("/report/{id}")
        public Result<AssessmentReportVO> report(@PathVariable Long id) { return Result.success(new AssessmentReportVO()); }
        @GetMapping("/history")
        public Result<List<AssessmentReportVO>> history() { return Result.success(Collections.emptyList()); }
    }
    """,
    "trade": """
    package com.evbattery.modules.trade.controller;
    import com.evbattery.common.result.Result;
    import com.evbattery.modules.trade.dto.*;
    import com.evbattery.modules.trade.vo.*;
    import org.springframework.web.bind.annotation.*;
    import java.util.Collections;
    import java.util.List;
    @RestController
    @RequestMapping("/api/trade")
    public class TradeController {
        @PostMapping("/product/publish")
        public Result<String> publishProduct(@RequestBody ProductPublishDTO dto) { return Result.success("发布商品接口预留", null); }
        @PostMapping("/demand/publish")
        public Result<String> publishDemand(@RequestBody DemandPublishDTO dto) { return Result.success("发布需求接口预留", null); }
        @GetMapping("/product/list")
        public Result<List<ProductVO>> productList() { return Result.success(Collections.emptyList()); }
        @GetMapping("/demand/list")
        public Result<List<DemandVO>> demandList() { return Result.success(Collections.emptyList()); }
        @PostMapping("/order/place")
        public Result<String> placeOrder(@RequestBody OrderCreateDTO dto) { return Result.success("下单接口预留", null); }
        @PostMapping("/order/pay/{orderId}")
        public Result<String> pay(@PathVariable Long orderId) { return Result.success("模拟支付成功", null); }
        @PostMapping("/order/ship/{orderId}")
        public Result<String> ship(@PathVariable Long orderId) { return Result.success("发货确认接口预留", null); }
        @PostMapping("/order/review")
        public Result<String> review(@RequestBody OrderReviewDTO dto) { return Result.success("收货评价接口预留", null); }
    }
    """,
    "contract": """
    package com.evbattery.modules.contract.controller;
    import com.evbattery.common.result.Result;
    import com.evbattery.modules.contract.dto.ContractGenerateDTO;
    import org.springframework.web.bind.annotation.*;
    @RestController
    @RequestMapping("/api/contract")
    public class ContractController {
        @PostMapping("/generate")
        public Result<String> generate(@RequestBody ContractGenerateDTO dto) { return Result.success("http://mock/contract/demo.pdf"); }
        @GetMapping("/download/{id}")
        public Result<String> download(@PathVariable Long id) { return Result.success("合同下载接口预留", null); }
        @GetMapping("/verify/{id}")
        public Result<String> verify(@PathVariable Long id) { return Result.success("哈希验证通过(模拟)", null); }
    }
    """,
    "logistics": """
    package com.evbattery.modules.logistics.controller;
    import com.evbattery.common.result.Result;
    import com.evbattery.modules.logistics.dto.TrackingFillDTO;
    import com.evbattery.modules.logistics.vo.LogisticsVO;
    import org.springframework.web.bind.annotation.*;
    @RestController
    @RequestMapping("/api/logistics")
    public class LogisticsController {
        @PostMapping("/fill-tracking")
        public Result<String> fill(@RequestBody TrackingFillDTO dto) { return Result.success("填写运单号接口预留", null); }
        @GetMapping("/status/{orderId}")
        public Result<LogisticsVO> status(@PathVariable Long orderId) { return Result.success(new LogisticsVO()); }
        @GetMapping("/hazardous-notice/{orderId}")
        public Result<String> notice(@PathVariable Long orderId) { return Result.success("危险品运输告知单(模拟文本)", null); }
    }
    """,
    "admin": """
    package com.evbattery.modules.admin.controller;
    import com.evbattery.common.result.Result;
    import com.evbattery.modules.admin.dto.*;
    import com.evbattery.modules.admin.vo.DashboardVO;
    import org.springframework.web.bind.annotation.*;
    import java.util.Collections;
    import java.util.List;
    import java.util.Map;
    @RestController
    @RequestMapping("/api/admin")
    public class AdminController {
        @GetMapping("/dashboard")
        public Result<DashboardVO> dashboard() {
            DashboardVO vo = new DashboardVO();
            vo.setMessage("模拟统计数据");
            vo.setDetail(Map.of("tradeCount", 120, "healthA", 38, "healthB", 51));
            return Result.success(vo);
        }
        @GetMapping("/users")
        public Result<List<Object>> users() { return Result.success(Collections.emptyList()); }
        @PostMapping("/users")
        public Result<String> createUser(@RequestBody UserManageDTO dto) { return Result.success("用户新增接口预留", null); }
        @PutMapping("/users")
        public Result<String> updateUser(@RequestBody UserManageDTO dto) { return Result.success("用户修改接口预留", null); }
        @DeleteMapping("/users/{id}")
        public Result<String> deleteUser(@PathVariable Long id) { return Result.success("用户删除接口预留", null); }
        @PostMapping("/battery/audit")
        public Result<String> auditBattery(@RequestBody BatteryAuditDTO dto) { return Result.success("电池档案审核接口预留", null); }
        @GetMapping("/orders")
        public Result<List<Object>> orders() { return Result.success(Collections.emptyList()); }
    }
    """,
    "statistics": """
    package com.evbattery.modules.statistics.controller;
    import com.evbattery.common.result.Result;
    import org.springframework.web.bind.annotation.*;
    import java.util.List;
    import java.util.Map;
    @RestController
    @RequestMapping("/api/statistics")
    public class StatisticsController {
        @GetMapping("/trade-trend")
        public Result<Object> tradeTrend() { return Result.success(Map.of("x", List.of("Mon", "Tue"), "y", List.of(12, 18))); }
        @GetMapping("/health-distribution")
        public Result<Object> healthDistribution() { return Result.success(Map.of("A", 30, "B", 50, "C", 20)); }
        @GetMapping("/source-distribution")
        public Result<Object> sourceDistribution() { return Result.success(Map.of("4S店", 45, "换电站", 55)); }
    }
    """,
    "report": """
    package com.evbattery.modules.report.controller;
    import com.evbattery.common.result.Result;
    import com.evbattery.modules.report.dto.ReportGenerateDTO;
    import com.evbattery.modules.report.vo.ReportCompareVO;
    import com.evbattery.modules.report.vo.ReportVO;
    import org.springframework.web.bind.annotation.*;
    import java.util.Collections;
    import java.util.List;
    @RestController
    @RequestMapping("/api/report")
    public class ReportController {
        @PostMapping("/generate")
        public Result<String> generate(@RequestBody ReportGenerateDTO dto) { return Result.success("报告生成任务已受理", null); }
        @GetMapping("/list")
        public Result<List<ReportVO>> list() { return Result.success(Collections.emptyList()); }
        @GetMapping("/compare")
        public Result<ReportCompareVO> compare(@RequestParam Long id1, @RequestParam Long id2) { return Result.success(new ReportCompareVO()); }
    }
    """,
}
for mod, content in controllers.items():
    cname = mod[:1].upper() + mod[1:] + "Controller.java"
    if mod == "trade":
        cname = "TradeController.java"
    elif mod == "admin":
        cname = "AdminController.java"
    elif mod == "user":
        continue
    write(f"{backend_base}/src/main/java/com/evbattery/modules/{mod}/controller/{cname}", content)

write(
    f"{frontend_base}/src/router/index.js",
    """
    import { createRouter, createWebHistory } from 'vue-router'
    import { useUserStore } from '../store/user'
    const routes = [
      { path: '/login', component: () => import('../views/auth/Login.vue') },
      { path: '/register', component: () => import('../views/auth/Register.vue') },
      {
        path: '/',
        component: () => import('../layout/Layout.vue'),
        children: [
          { path: '', component: () => import('../views/home/Home.vue') },
          { path: 'battery/list', component: () => import('../views/battery/BatteryList.vue') },
          { path: 'battery/detail/:id', component: () => import('../views/battery/BatteryDetail.vue') },
          { path: 'battery/upload', component: () => import('../views/battery/BatteryUpload.vue') },
          { path: 'assessment/list', component: () => import('../views/assessment/AssessmentList.vue') },
          { path: 'assessment/report/:id', component: () => import('../views/assessment/AssessmentReport.vue') },
          { path: 'trade/product-list', component: () => import('../views/trade/ProductList.vue') },
          { path: 'trade/demand-list', component: () => import('../views/trade/DemandList.vue') },
          { path: 'trade/order-list', component: () => import('../views/trade/OrderList.vue') },
          { path: 'contract/list', component: () => import('../views/contract/ContractList.vue') },
          { path: 'logistics/list', component: () => import('../views/logistics/LogisticsList.vue') },
          { path: 'admin/dashboard', component: () => import('../views/admin/Dashboard.vue') },
          { path: 'admin/user', component: () => import('../views/admin/UserManage.vue') },
          { path: 'statistics/overview', component: () => import('../views/statistics/Overview.vue') },
          { path: 'report/list', component: () => import('../views/report/ReportList.vue') }
        ]
      }
    ]
    const router = createRouter({ history: createWebHistory(), routes })
    router.beforeEach((to, from, next) => {
      const userStore = useUserStore()
      if (to.path !== '/login' && to.path !== '/register' && !userStore.token) return next('/login')
      next()
    })
    export default router
    """,
)

apis = ["user", "battery", "assessment", "trade", "contract", "logistics", "admin", "statistics", "report"]
api_map = {
    "user": "import request from '../utils/request'\nexport const login = (data) => request.post('/user/login', data)\nexport const register = (data) => request.post('/user/register', data)\nexport const getCurrentUser = () => request.get('/user/current')\nexport const changePassword = (data) => request.post('/user/change-password', data)\n",
    "battery": "import request from '../utils/request'\nexport const uploadSingle = (data) => request.post('/battery/upload/single', data)\nexport const uploadBatch = (data) => request.post('/battery/upload/batch', data)\nexport const getBatteryList = (params) => request.get('/battery/list', { params })\nexport const getBatteryDetail = (id) => request.get(`/battery/${id}`)\n",
    "assessment": "import request from '../utils/request'\nexport const triggerAssessment = (batteryId) => request.post('/assessment/trigger', { batteryId })\nexport const getAssessmentReport = (id) => request.get(`/assessment/report/${id}`)\nexport const getAssessmentHistory = (params) => request.get('/assessment/history', { params })\n",
    "trade": "import request from '../utils/request'\nexport const publishProduct = (data) => request.post('/trade/product/publish', data)\nexport const publishDemand = (data) => request.post('/trade/demand/publish', data)\nexport const getProductList = (params) => request.get('/trade/product/list', { params })\nexport const getDemandList = (params) => request.get('/trade/demand/list', { params })\nexport const placeOrder = (data) => request.post('/trade/order/place', data)\nexport const payOrder = (id) => request.post(`/trade/order/pay/${id}`)\nexport const shipOrder = (id) => request.post(`/trade/order/ship/${id}`)\nexport const reviewOrder = (data) => request.post('/trade/order/review', data)\n",
    "contract": "import request from '../utils/request'\nexport const generateContract = (orderId) => request.post('/contract/generate', { orderId })\nexport const downloadContract = (id) => request.get(`/contract/download/${id}`)\nexport const verifyHash = (id) => request.get(`/contract/verify/${id}`)\n",
    "logistics": "import request from '../utils/request'\nexport const fillTracking = (data) => request.post('/logistics/fill-tracking', data)\nexport const queryStatus = (orderId) => request.get(`/logistics/status/${orderId}`)\nexport const hazardousNotice = (orderId) => request.get(`/logistics/hazardous-notice/${orderId}`)\n",
    "admin": "import request from '../utils/request'\nexport const getDashboard = () => request.get('/admin/dashboard')\nexport const getUsers = (params) => request.get('/admin/users', { params })\nexport const createUser = (data) => request.post('/admin/users', data)\nexport const updateUser = (data) => request.put('/admin/users', data)\nexport const deleteUser = (id) => request.delete(`/admin/users/${id}`)\nexport const getOrders = (params) => request.get('/admin/orders', { params })\nexport const auditBattery = (data) => request.post('/admin/battery/audit', data)\n",
    "statistics": "import request from '../utils/request'\nexport const tradeTrend = (params) => request.get('/statistics/trade-trend', { params })\nexport const healthDistribution = (params) => request.get('/statistics/health-distribution', { params })\nexport const sourceDistribution = (params) => request.get('/statistics/source-distribution', { params })\n",
    "report": "import request from '../utils/request'\nexport const generateReport = (data) => request.post('/report/generate', data)\nexport const listReport = (params) => request.get('/report/list', { params })\nexport const compareReport = (id1, id2) => request.get('/report/compare', { params: { id1, id2 } })\n",
}
for n in apis:
    write(f"{frontend_base}/src/api/{n}.js", api_map[n])

view_specs = {
    "auth/Login.vue": """
    <template>
      <div style="max-width: 420px; margin: 80px auto;">
        <el-card>
          <h3>登录</h3>
          <el-form :model="form">
            <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
            <el-form-item label="密码"><el-input type="password" v-model="form.password" /></el-form-item>
            <el-button type="primary" @click="handleLogin">登录</el-button>
            <el-button link @click="$router.push('/register')">注册</el-button>
          </el-form>
        </el-card>
      </div>
    </template>
    <script setup>
    import { reactive } from 'vue'
    import { useRouter } from 'vue-router'
    import { login } from '../../api/user'
    import { useUserStore } from '../../store/user'
    const router = useRouter()
    const userStore = useUserStore()
    const form = reactive({ username: '', password: '' })
    const handleLogin = async () => {
      const res = await login(form)
      const token = res?.data?.detail?.token || ''
      if (token) {
        userStore.setToken(token)
        router.push('/')
      }
    }
    </script>
    """,
    "auth/Register.vue": "<template><div>注册模块开发中</div></template>",
    "home/Home.vue": "<template><div>首页模块开发中</div></template>",
    "battery/BatteryList.vue": "<template><div>电池列表模块开发中</div></template>",
    "battery/BatteryDetail.vue": "<template><div>电池详情模块开发中</div></template>",
    "battery/BatteryUpload.vue": "<template><div>电池上传模块开发中</div></template>",
    "assessment/AssessmentList.vue": "<template><div>评估列表模块开发中</div></template>",
    "assessment/AssessmentReport.vue": "<template><div>评估报告模块开发中</div></template>",
    "trade/ProductList.vue": "<template><div>商品列表模块开发中</div></template>",
    "trade/DemandList.vue": "<template><div>需求列表模块开发中</div></template>",
    "trade/OrderList.vue": "<template><div>订单列表模块开发中</div></template>",
    "contract/ContractList.vue": "<template><div>合同模块开发中</div></template>",
    "logistics/LogisticsList.vue": "<template><div>物流模块开发中</div></template>",
    "admin/Dashboard.vue": "<template><div>管理仪表盘模块开发中</div></template>",
    "admin/UserManage.vue": "<template><div>用户管理模块开发中</div></template>",
    "statistics/Overview.vue": "<template><div>统计可视化模块开发中</div></template>",
    "report/ReportList.vue": "<template><div>智能报告模块开发中</div></template>",
}
for rel, c in view_specs.items():
    write(f"{frontend_base}/src/views/{rel}", c)

write(
    f"{frontend_base}/nginx.conf",
    """
    server {
        listen 80;
        server_name localhost;
        location / {
            root /usr/share/nginx/html;
            index index.html;
            try_files $uri $uri/ /index.html;
        }
        location /api/ {
            proxy_pass http://backend:8080/api/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }
    }
    """,
)

print("Scaffold generated.")
