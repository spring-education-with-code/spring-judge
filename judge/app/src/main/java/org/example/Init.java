package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static org.example.App.redisClient;

public class Init {

    // 환경 변수를 설정하는
    public void setProperties(){
        Properties props = new Properties();

        try (FileInputStream fis = new FileInputStream("config.properties")) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("config.properties 파일을 읽을 수 없습니다: " + e.getMessage());
            return;
        }

        App.DB_URL = props.getProperty("db.url");
        App.DB_USER = props.getProperty("db.user");
        App.DB_PASSWORD = props.getProperty("db.password");
    }


    public void connectRedis(){
        App.redisClient = redisClient.create("redis://localhost:6379");
        App.redisConnection = redisClient.connect();
    }
}
