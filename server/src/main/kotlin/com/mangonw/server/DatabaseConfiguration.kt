package com.mangonw.server

import com.zaxxer.hikari.HikariDataSource
import org.apache.ibatis.session.SqlSession
import org.apache.ibatis.session.SqlSessionFactory
import org.mybatis.spring.SqlSessionFactoryBean
import org.mybatis.spring.SqlSessionTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import javax.sql.DataSource

@Configuration
class DatabaseConfiguration {

    @Bean
    fun servletWebServerFactory(): ServletWebServerFactory? {
        return TomcatServletWebServerFactory()
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari.james")
    fun dataSource(): DataSource? {
        return DataSourceBuilder.create().type(HikariDataSource::class.java).build()
    }

    @Autowired
    @Qualifier(value = "dataSource")
    private val dataSource: DataSource? = null

    @Bean
    @Throws(Exception::class)
    fun sqlSessionFactory(): SqlSessionFactory? {
        val sqlSessionFactoryBean = SqlSessionFactoryBean()
        sqlSessionFactoryBean.setDataSource(dataSource)
        val resolver = PathMatchingResourcePatternResolver()
        val resources = resolver.getResources("classpath*:mybatis/**/*.xml")
        sqlSessionFactoryBean.setMapperLocations(*resources)
        return sqlSessionFactoryBean.getObject()
    }

    @Bean
    @Throws(Exception::class)
    fun sqlSession(): SqlSession? {
        return SqlSessionTemplate(sqlSessionFactory())
    }
}