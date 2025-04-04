package org.example;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Slf4j
public class TestFileUtility {
    public int countTests(){

        int count = 0;

        Path filePath = Paths.get("../src/test/java/com/spring_education/template");

        try (Stream<Path> paths = Files.walk(filePath)) {
            count = paths.filter(Files::isRegularFile)
                    // 파일 이름이 "Test"로 시작하고 ".java"로 끝나는지 필터링
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.startsWith("Test") && fileName.endsWith(".java");
                    })
                    .mapToInt(path -> {
                        try{
                            String content = new String(Files.readAllBytes(path));
                            int temp_count = 0;
                            int index = content.indexOf("@Test");

                            while (index != -1) {
                                temp_count ++;
                                index = content.indexOf("@Test", index + 1);
                            }

                            return temp_count;
                        }catch(IOException e){
                            log.info(e.getMessage());
                            return 0;
                        }
                    })
                    .sum();
        }catch(IOException e){
            log.info(e.getMessage());
        }

        return count;
    }
}
