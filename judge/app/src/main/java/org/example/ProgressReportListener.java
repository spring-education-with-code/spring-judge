package org.example;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

public class ProgressReportListener implements TestExecutionListener {
    private int totalTests;
    private int finishedTests;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        totalTests = (int) testPlan.countTestIdentifiers(TestIdentifier::isTest);
        System.out.println("샤샤샤");
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            System.out.println("Test started: " + testIdentifier.getDisplayName());
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest()) {
            finishedTests++;
            // 중간 진행 상황 RabbitMQ로 전송
            // ex) sendProgress(finishedTests, totalTests);
        }
    }
}
