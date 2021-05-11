package com.comerzzia.core.util.db;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.ibatis.datasource.pooled.PooledDataSourceFactory;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.BooleanTypeHandler;
import org.apache.ibatis.type.TypeHandler;
import org.apache.log4j.Logger;
import org.mybatis.spring.SqlSessionFactoryBean;

import com.comerzzia.core.util.config.ComerzziaApp;
import com.comerzzia.core.util.config.DBInfo;
import com.comerzzia.core.util.mybatis.DynamicMappersSqlSessionFactory;
import com.comerzzia.core.util.mybatis.interceptors.BlankStringToNullInterceptor;
import com.comerzzia.core.util.mybatis.typehandlers.BooleanNumberTypeHandler;
import com.comerzzia.core.util.mybatis.typehandlers.BooleanStringTypeHandler;
import com.comerzzia.core.util.mybatis.typehandlers.CustomBlobTypeHandler;

public class DefaultConnectionProvider implements IConnectionProvider {
	private static Logger log = Logger.getLogger(DefaultConnectionProvider.class);
				
	private static SqlSessionFactory sqlSessionFactory;
	private static SqlSessionFactory springSqlSessionFactory;
	private static SqlSessionFactoryBean springMybatisFactoryBean;
	
	private DataSource dataSource;
	
	public SqlSessionFactory getSqlSessionFactory() {
		if (sqlSessionFactory == null) {
			initSqlSessionFactory();	
		}
		return sqlSessionFactory;
	}
	
	public SqlSessionFactory getSpringSqlSessionFactory() {
		if (springSqlSessionFactory == null) {
			initSqlSessionFactory();	
		}
		return springSqlSessionFactory;
	}

	public DataSource getDataSource() {
	   return dataSource; 
    }
	
	private void initSqlSessionFactory() {
		LogFactory.useLog4JLogging();
		
		String datasourceName = ComerzziaApp.get().getDbInfo().getDatasource();
   		
   		if (datasourceName != null && !datasourceName.isEmpty()) {
   			createSessionFactoryFromDatasource();
   		} else {
   			createSessionFactoryFromPool();
   			
   		}		
	}
	    		
	private void createSessionFactoryFromDatasource()  {
		log.info("Creando Session Factory usando el datasource jndi [" + ComerzziaApp.get().getDbInfo().getDatasource() + "]");		
		try {
			Context ctx = new InitialContext();
   		    dataSource = (DataSource) ctx.lookup(ComerzziaApp.get().getDbInfo().getDatasource());
		}
		catch (NamingException e) {
			throw new RuntimeException("Error al obtener configuracion de la base de datos: " + e.getMessage());
		}
				
		TransactionFactory transactionFactory = new JdbcTransactionFactory();
		
		log.debug("\tCreando environment...");
		Environment environment = new Environment("MyBatis-Environment", transactionFactory, dataSource);
		Configuration configuration = new Configuration(environment);
		//Configuramos databaseId para diferenciar statements en los Mappers. Valores posibles: "Oracle", "MySQL", "Microsoft SQL Server" 
        //y otros valores de las implementaciones de java.util.DatabaseMetaData.getDatabaseProductName()
        configuration.setDatabaseId(new VendorDatabaseIdProvider().getDatabaseId(dataSource));
        if(!configuration.getDatabaseId().contains("Oracle")) {
        	configuration.addInterceptor(new BlankStringToNullInterceptor());
        }
		log.debug("\tAñadiendo TypeHandlers...");
		addTypeHandlers(configuration);
		
        sqlSessionFactory = new DynamicMappersSqlSessionFactory(new SqlSessionFactoryBuilder().build(configuration));
		
		createSpringFactoryBean();
	}
	
	private void createSessionFactoryFromPool() {
		DBInfo dbInfo = ComerzziaApp.get().getDbInfo();
		
		log.info("Creando Session Factory usando un pool de conexiones de MyBatis hacia [" + ComerzziaApp.get().getDbInfo().getDatabaseUrl() + "]");		

        PooledDataSourceFactory dataSourceFactory = new PooledDataSourceFactory();

        Properties prop = new Properties();
        prop.put("driver", dbInfo.getDatabaseDriverClass());
        prop.put("username", dbInfo.getDatabaseUsuario());
        prop.put("password", dbInfo.getDatabasePassword());
        prop.put("poolMaximumActiveConnections", dbInfo.getDatabaseMaxConexionesActivas().toString());
        
        
        if (!dbInfo.getDatabaseCheckConnectionSQL().isEmpty()) {		       
	       prop.put("poolPingQuery", dbInfo.getDatabaseCheckConnectionSQL());
	       prop.put("poolPingEnabled", "true");
	       log.info("\tDatabase ping SQL query active: " + dbInfo.getDatabaseCheckConnectionSQL());
	    }
        
        String url = ComerzziaApp.get().getDbInfo().getDatabaseUrl();
	    if(url.toUpperCase().contains("MYSQL")) {
	    	String parametrosMySQL = "";	        
	        String parametros = ComerzziaApp.get().getDbInfo().getParametrosAuxiliares();
	        if(parametros != null){
	        	parametros = parametros.trim();
				if (!parametros.isEmpty()){
					String[] parametrosArray = parametros.split(";");
					for (String parametro : parametrosArray) {
						if(parametrosMySQL.isEmpty()) {
							parametrosMySQL = parametro;
						}
						else{
							parametrosMySQL = parametrosMySQL + "&" + parametro;
						}
		            }
					
					url = url + "?" + parametrosMySQL;
				}
	        }
			
	    }
	    prop.put("url", url);
	    
        dataSourceFactory.setProperties(prop);
        
        dataSource = dataSourceFactory.getDataSource();
        
        TransactionFactory transactionFactory = new JdbcTransactionFactory();        
		
        log.debug("\tCreando environment...");
        Environment environment = new Environment("development", transactionFactory, dataSourceFactory.getDataSource());
        Configuration configuration = new Configuration(environment);
        //Configuramos databaseId para diferenciar statements en los Mappers. Valores posibles: "Oracle", "MySQL", "Microsoft SQL Server" 
        //y otros valores de las implementaciones de java.util.DatabaseMetaData.getDatabaseProductName()
        configuration.setDatabaseId(new VendorDatabaseIdProvider().getDatabaseId(dataSourceFactory.getDataSource()));
        if(!configuration.getDatabaseId().contains("Oracle")) {
        	configuration.addInterceptor(new BlankStringToNullInterceptor());
        }
        log.debug("\tAñadiendo TypeHandlers...");
        addTypeHandlers(configuration);
                
        sqlSessionFactory = new DynamicMappersSqlSessionFactory(new SqlSessionFactoryBuilder().build(configuration));
        
        createSpringFactoryBean();
    }
	
	
	protected void addTypeHandlers(Configuration configuration){
		configuration.getTypeHandlerRegistry().register(BooleanTypeHandler.class);
		configuration.getTypeHandlerRegistry().register(BooleanStringTypeHandler.class);
		configuration.getTypeHandlerRegistry().register(CustomBlobTypeHandler.class); // TODO edu: registro el typehandler
	}	
	
	private void createSpringFactoryBean() {
		springMybatisFactoryBean = new SqlSessionFactoryBean();                
		springMybatisFactoryBean.setDataSource(dataSource);
		springMybatisFactoryBean.setTypeHandlers(new TypeHandler<?>[]{new BooleanTypeHandler()});		
		springMybatisFactoryBean.setTypeHandlers(new TypeHandler<?>[]{new BooleanStringTypeHandler()});
		springMybatisFactoryBean.setTypeHandlers(new TypeHandler<?>[]{new BooleanNumberTypeHandler()});
		springMybatisFactoryBean.setTypeHandlers(new TypeHandler<?>[]{new CustomBlobTypeHandler()}); // TODO edu añado el TypeHandler
		Configuration configuration = new Configuration();
		configuration.setDatabaseId(new VendorDatabaseIdProvider().getDatabaseId(dataSource));
		springMybatisFactoryBean.setConfiguration(configuration);
		try {
			springSqlSessionFactory = springMybatisFactoryBean.getObject();
		} catch (Exception e) {
			throw new RuntimeException("createSpringFactoryBean() Error creaando SqlSessionFactoryBean: " + e.getMessage());
		}
	}
}
