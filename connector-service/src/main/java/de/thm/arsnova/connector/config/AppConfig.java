package de.thm.arsnova.connector.config;

import java.sql.SQLException;
import java.util.Properties;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.OpenJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import de.thm.arsnova.connector.dao.ConnectorDao;

@ComponentScan(basePackages = {
		"de.thm.arsnova.connector.dao",
		"de.thm.arsnova.connector.services"
})
@Configuration
@EnableJpaRepositories("de.thm.arsnova.connector.persistence.repository")
@PropertySource("file:///etc/arsnova/connector.properties")
public class AppConfig {

	@Autowired
	private Environment env;

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean(name = "dataSource")
	public DriverManagerDataSource dataSource() throws SQLException {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(env.getProperty("jdbc.driverClassName"));
		dataSource.setUrl(env.getProperty("jdbc.url"));
		dataSource.setUsername(env.getProperty("jdbc.username"));
		dataSource.setPassword(env.getProperty("jdbc.password"));
		return dataSource;
	}

	@Bean(name = "configDataSource")
	public HsqlDataSource configDataSource() throws SQLException {
		HsqlDataSource dataSource = new HsqlDataSource();
		dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
		dataSource.setUrl("jdbc:hsqldb:file:/etc/arsnova/connector.db");
		dataSource.setUsername("whatever");
		dataSource.setPassword("topsecret");
		return dataSource;
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() throws SQLException {
		LocalContainerEntityManagerFactoryBean lef = new LocalContainerEntityManagerFactoryBean();
		lef.setDataSource(configDataSource());
		lef.setJpaVendorAdapter(jpaVendorAdapter());
		lef.setPackagesToScan("de.thm.arsnova.connector.persistence.domain");
		Properties jpaProperties = new Properties();
		jpaProperties.put("openjpa.RuntimeUnenhancedClasses", "supported");
		lef.setJpaProperties(jpaProperties);
		lef.afterPropertiesSet();
		return lef;
	}

	@Bean
	public JpaVendorAdapter jpaVendorAdapter() {
		OpenJpaVendorAdapter jpaVendorAdapter = new OpenJpaVendorAdapter();
		jpaVendorAdapter.setShowSql(false);
		jpaVendorAdapter.setGenerateDdl(true);
		return jpaVendorAdapter;
	}

	@Bean
	public PlatformTransactionManager transactionManager() throws SQLException {
		JpaTransactionManager txManager = new JpaTransactionManager();
		txManager.setEntityManagerFactory(entityManagerFactory().getObject());
		return txManager;
	}

	@Bean
	public ConnectorDao connectorDao() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		return (ConnectorDao) Class.forName(env.getProperty("dao.implementation")).newInstance();
	}

	private class HsqlDataSource extends DriverManagerDataSource {
		@PreDestroy
		public void shutdown() {
			logger.info("Shutting down HSQLDB");
			try {
				this.getConnection().createStatement().execute("SHUTDOWN;");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
