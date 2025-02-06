package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.DeliverCallback;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class RabbitmqUtility {
    ObjectMapper objectMapper;
    DBUtility dbUtility;

    RabbitmqUtility(){
        objectMapper = new ObjectMapper();
        dbUtility = new DBUtility();
    }

    //dto 관련 변수

    //rabbitmq 에 메세지 도착 시 실행되는 메서드 진입점
    public DeliverCallback createDeliverCallback(){
        return (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");

            //message dto 형태로 분석
            Map<String, String> dtoMap = objectMapper.readValue(message, Map.class);
            System.out.println(" [x] Received '" + dtoMap.get("service") + "'");

            //알맞은 디렉토리에 사용자 코드 (controller, service) 를 붙여넣기
            updateContent(dtoMap);

            if(!dbUtility.isThereSameMySqlSubmit(dtoMap)){
                //스프링 빌드
                int exitCode = build(dtoMap);
                //db 업데이트
                dbUtility.updateSQL(dtoMap, exitCode);
            }
        };
    }

    // 알맞은 디렉토리에 사용자 코드 (controller, service) 를 붙여넣기
    public void updateContent(Map<String, String> dtoMap){
        try {

            //dtoMap 에서 controller 받은 코드 파일에 덮어쓰기
            String basicFilePath = "../src/main/java/com/spring_education/template";
            String filePath = basicFilePath + "/controller/TestController.java";
            Files.write(Paths.get(filePath), dtoMap.get("controller").getBytes(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            //dtoMap 에서 service 받은 코드 파일에 덮어쓰기
            filePath = basicFilePath + "/service/TestService.java";
            Files.write(Paths.get(filePath), dtoMap.get("service").getBytes(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    // 빌드 한다
    public int build(Map<String, String> dtoMap){
        // 빌드
        try{
            String gradlewPath = "./gradlew";

            ProcessBuilder processBuilder = new ProcessBuilder(gradlewPath, "build");
            // directory 설정 필수(!!). 이렇게 해야 스프링부트의 graldew 설정하여 잘 작동함
            processBuilder.directory(new File(".."));
            Process process = processBuilder.start();

            // 로그 (gradlew build 하면서)
            String line;
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            //에러 로그 (gradlew build 하면서)
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                while ((line = errorReader.readLine()) != null) {
                    System.err.println(line);
                }
            }

            // 프로세스 종료 대기 및 종료 코드 확인
            int exitCode = process.waitFor();
            System.out.println("프로세스 종료 코드: " + exitCode);

            return exitCode;

        }catch(IOException e){
            e.printStackTrace();
            return -1;
        }catch(InterruptedException e){
            e.printStackTrace();
            return -1;
        }
    }

}
