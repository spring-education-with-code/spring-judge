package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import lombok.extern.slf4j.Slf4j;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.ProgressListener;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import org.junit.platform.launcher.TestPlan;

@Slf4j
public class RabbitmqUtility {

    ObjectMapper objectMapper;
    DBUtility dbUtility;
    TestFileUtility testFileUtility;

    RabbitmqUtility(){
        objectMapper = new ObjectMapper();
        dbUtility = new DBUtility();
        testFileUtility = new TestFileUtility();
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

            int isCorrect;
            int isThereSameRedisSubmit = dbUtility.isThereSameRedisSubmit(dtoMap);
            if(isThereSameRedisSubmit != -1) {
                //redis 에 이미 값이 있는 경우
                System.out.println("레디스에 기존 제출이 있습니다");
                isCorrect = isThereSameRedisSubmit;
            }else{
                //redis 에 이미 값이 없는 경우
                int isThereSameMySqlSubmit = dbUtility.isThereSameMySqlSubmit(dtoMap);
                if(isThereSameMySqlSubmit != -1){
                    //redis 에는 없으나 mysql 에는 기존 제출이 있는 경우
                    System.out.println("mysql에 기존 제출이 있습니다");
                    isCorrect = isThereSameMySqlSubmit;

                }else{
                    //스프링 빌드
                    int exitCode = build(dtoMap, "1");

                    if(exitCode == 0){
                        isCorrect = 1;
                    }else{
                        isCorrect = 0;
                    }

                    //db 제출 이력 삽입
                    dbUtility.insertSQL(dtoMap, isCorrect);
                }
                //redis에 제출 이력 삽입
                dbUtility.insertRedis(dtoMap, isCorrect);
            }
            //isCorrect 여부를 rabbitmq로 보낸다
            System.out.println("rabbitmq 로 채점 결과를 전송해야함 ~ " + "정답 여부는 : " + isCorrect );
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

    // 빌드 한다 (result 0 이 정답인거)
    public int build(Map<String, String> dtoMap, String userId){

        log.info("build 함수 실행");

        final AtomicInteger result = new AtomicInteger(0);
        final AtomicInteger passedTestCount = new AtomicInteger(0);
        int totalTestCount = testFileUtility.countTests();

        File projectDir = new File("..");

        // Gradle 프로젝트에 연결
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .connect()) {

            // "test" 태스크를 실행할 BuildLauncher 생성
            BuildLauncher buildLauncher = connection.newBuild();
            buildLauncher.forTasks("clean", "test");

            // 진행 상황 리스너 등록
            buildLauncher.addProgressListener(new ProgressListener() {
                @Override
                public void statusChanged(ProgressEvent event) {
                    // <System.out.println("Progress event: " + event.getDisplayName()); 의 출력형태>
                    // 테스트 성공 시
                    // Progress event: Test 더미_테스트_1()(com.spring_education.template.Test1) started
                    // Progress event: Test 더미_테스트_1()(com.spring_education.template.Test1) succeeded
                    // 테스트 실패 시
                    // Progress event: Test class com.spring_education.template.Test1 failed
                    String nowEvent = event.getDisplayName();

                    if (nowEvent.contains("case") && nowEvent.contains("succeeded")) {
                        sendResults(makeResultsMessage(1,1, passedTestCount.incrementAndGet(), totalTestCount));
                        log.info("test succeed 로그" + passedTestCount.get() + "번째 테스트가 통과되었습니다" + "원본 메세지: " + nowEvent);
                    }

                    if (nowEvent.contains("case") && nowEvent.contains("failed")) {
                        sendResults(makeResultsMessage(1,1, -1, totalTestCount));
                        result.set(1);
                        log.info("test failed 로그");
                    }

                }
            });

            // 태스크 실행 (실행 중 진행 이벤트가 리스너에 의해 출력됨)
            buildLauncher.run();
            System.out.println("테스트 실행 완료");
        }

        return result.get();
    }

    public String makeResultsMessage(int userId, int problemId, int solvedTestNum, int totalTestNum){
        //problemId 는 필요 없을 지도..
        ResultDTO resultDTO = new ResultDTO(userId, problemId, solvedTestNum, totalTestNum);
        String message = "";
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            message = objectMapper.writeValueAsString(resultDTO);
        }catch(JsonProcessingException e){
            log.error(e.getMessage());
        }

        return message;
    }

    //rabbitmq 에 (중간) 결과를 보내기
    public void sendResults(String message){

        String QUEUE_NAME = "spring.education.result";

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // 메시지 발행 (exchange는 기본값인 빈 문자열을 사용)
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            log.info(" [x] Sent '" + message + "'");
        }catch(IOException e){
            log.error(e.getMessage());
        }catch(TimeoutException e){
            log.error(e.getMessage());
        }
    }
}

