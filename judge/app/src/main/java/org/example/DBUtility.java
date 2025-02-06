package org.example;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class DBUtility {

    public boolean isThereSameMySqlSubmit(Map<String, String> dtoMap){
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
                    System.out.println("제출한 코드와 같습니다.");
                    return true;
                }
            }

        } catch (SQLException e) {
            System.err.println("DB 삽입 중 오류 발생: " + e.getMessage());
        }

        return false;
    }


    public void updateSQL(Map<String, String> dtoMap, int exitCode){
        // 채점결과에 따라 db 에 반영하는 부분
        String problemSubmitId = dtoMap.get("problem_submit_id");
        String updateSQL = "update problem_submit set is_correct = ? where problem_submit_id = ?";

        try(java.sql.Connection conn = DriverManager.getConnection(App.DB_URL, App.DB_USER, App.DB_PASSWORD);
            PreparedStatement pstmt = conn.prepareStatement(updateSQL)){

            if(exitCode == 0){
                pstmt.setInt(1, 1);
            }else{
                pstmt.setInt(1,0);
            }
            pstmt.setString(2, problemSubmitId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("DB 삽입 중 오류 발생: " + e.getMessage());
        }
    }
}
