package org.example;

import io.lettuce.core.api.sync.RedisCommands;

import java.sql.*;
import java.util.Map;

public class DBUtility {

    Sha256 sha256 = new Sha256();

    public int isThereSameMySqlSubmit(Map<String, String> dtoMap){
        // 빌드 전에 이미 같은 제출 이력이 있는지 확인
        String controller = dtoMap.get("controller");
        String service = dtoMap.get("service");

        String selectSQL = "select * from problem_submit where user_id = ? and problem_id = ?";
        try(java.sql.Connection conn = DriverManager.getConnection(App.DB_URL, App.DB_USER, App.DB_PASSWORD);
            PreparedStatement pstmt = conn.prepareStatement(selectSQL)){

            pstmt.setString(1, dtoMap.get("user_id"));
            pstmt.setString(2, dtoMap.get("problem_id"));
            ResultSet rs = pstmt.executeQuery();

            while(rs.next()){
                if(controller.equals(rs.getString("controller_code")) && service.equals(rs.getString("service_code"))){
                    return rs.getInt("is_correct");
                }
            }

        } catch (SQLException e) {
            System.err.println("DB 삽입 중 오류 발생: " + e.getMessage());
        }

        return -1;
    }

    public int isThereSameRedisSubmit(Map<String, String> dtoMap){
        String userId = dtoMap.get("user_id");
        String problemId = dtoMap.get("problem_id");
        String controller = dtoMap.get("controller");
        String service = dtoMap.get("service");
        String codeHash = sha256.encrypt(controller + service);

        String key = "submit_cache:" + userId + ":" + problemId;
        RedisCommands<String, String> redisCommands = App.redisConnection.sync();

        String cachedResult = redisCommands.hget(key, codeHash);

        if(cachedResult != null){
            if(cachedResult.equals("1")){
                return 1;
            }else{
                return 0;
            }
        }
        return -1;
    }


    public long insertSQL(Map<String, String> dtoMap, int isCorrect){
        // 채점결과에 따라 db 에 반영하는 부분
        String userId = dtoMap.get("user_id");
        String problemId = dtoMap.get("problem_id");
        String controller_code = dtoMap.get("controller");
        String service_code = dtoMap.get("service");

        String insertSQL = "insert into problem_submit(user_id, problem_id, controller_code, service_code, is_correct) values(?,?,?,?,?)";

        long submitId = -1;

        try(java.sql.Connection conn = DriverManager.getConnection(App.DB_URL, App.DB_USER, App.DB_PASSWORD);
            PreparedStatement pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)){

            pstmt.setString(1, userId);
            pstmt.setString(2, problemId);
            pstmt.setString(3, controller_code);
            pstmt.setString(4, service_code);
            pstmt.setInt(5, isCorrect);
            pstmt.executeUpdate();


            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    submitId = rs.getLong(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("DB 삽입 중 오류 발생: " + e.getMessage());
        }

        return submitId;
    }

    public void updateSQL(long submitId, int isCorrect){
        String updateSQL = "update problem_submit set is_correct = ? where problem_submit_id = ?";

        try(java.sql.Connection conn = DriverManager.getConnection(App.DB_URL, App.DB_USER, App.DB_PASSWORD);
            PreparedStatement pstmt = conn.prepareStatement(updateSQL)){
            pstmt.setInt(1, isCorrect);
            pstmt.setLong(2, submitId);

            pstmt.executeUpdate();

        }catch(SQLException e){
            System.err.println("DB 갱신 중 오류 발생: " + e.getMessage());
        }
    }

    public void insertRedis(Map<String, String> dtoMap, int isCorrect){
        String userId = dtoMap.get("user_id");
        String problemId = dtoMap.get("problem_id");
        String controller = dtoMap.get("controller");
        String service = dtoMap.get("service");
        String codeHash = sha256.encrypt(controller + service);

        String key = "submit_cache:" + userId + ":" + problemId;
        RedisCommands<String, String> redisCommands = App.redisConnection.sync();

        redisCommands.hset(key, codeHash, String.valueOf(isCorrect));
    }
}
