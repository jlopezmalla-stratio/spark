package com.stratio.pnf.performance;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.qa.utils.ThreadProperty;
import com.stratio.spark.tests.utils.BaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PerformanceTests_IT extends BaseTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getCanonicalName());

    class TestCase {

        private Integer cores;
        private Integer coresPerExecutor;
        private String executorMemory;
        private String datasetPath;
        private Boolean historyServer;

        public TestCase(String[] csvLine) {

            this(
                    Integer.valueOf(csvLine[0]),
                    Integer.valueOf(csvLine[1]),
                    csvLine[2],
                    csvLine[3],
                    Boolean.valueOf(csvLine[4])
            );

        }

        public TestCase(Integer cores, Integer coresPerExecutor, String executorMemory, String datasetPath, Boolean historyServer) {
            this.cores = cores;
            this.coresPerExecutor = coresPerExecutor;
            this.executorMemory = executorMemory;
            this.datasetPath = datasetPath;
            this.historyServer = historyServer;
        }

        @Override
        public String toString() {
            return "TestCase{" +
                    "cores=" + cores +
                    ", coresPerExecutor=" + coresPerExecutor +
                    ", executorMemory='" + executorMemory + '\'' +
                    ", datasetPath='" + datasetPath + '\'' +
                    ", historyServer=" + historyServer +
                    '}';
        }

        public Integer getCores() {
            return cores;
        }

        public Integer getCoresPerExecutor() {
            return coresPerExecutor;
        }

        public String getExecutorMemory() {
            return executorMemory;
        }

        public String getDatasetPath() {
            return datasetPath;
        }

        public Boolean isHistoryServer() {
            return historyServer;
        }
    }

    private static String FEATURE = "pnf/performance/performance-test";
    private static String DEFAULT_CASES_CSV = "schemas/pnf/performance/execution_plan.csv";

    private static String CORES_VAR = "CORES";
    private static String CORES_PER_EXECUTOR_VAR = "CORES_PER_EXECUTOR";
    private static String EXECUTOR_MEMORY_VAR = "EXECUTOR_MEMORY";
    private static String DATASET_PATH_VAR = "DATASET_PATH";
    private static String HS_VAR = "HS_ENABLED";

    private static String EXTERNAL_CSV_ENV = "EXTERNAL_CSV";

    private List<TestCase> retrieveCases(File casesCsv) throws Exception {

        List<TestCase> testCases = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(casesCsv));
        //Omit first line
        br.readLine();

        String line = "";
        while ((line = br.readLine()) != null) {

            // use comma as separator
            String[] split = line.split(",");

            TestCase testCase = new TestCase(split);
            logger.info("Readed test case "+testCase);

            testCases.add(testCase);
        }


        return testCases;
    }

    private void setExecutionVariables(TestCase testCase) {
        ThreadProperty.set(CORES_VAR, testCase.getCores().toString());
        ThreadProperty.set(CORES_PER_EXECUTOR_VAR, testCase.getCoresPerExecutor().toString());
        ThreadProperty.set(EXECUTOR_MEMORY_VAR, testCase.getExecutorMemory());
        ThreadProperty.set(DATASET_PATH_VAR, testCase.getDatasetPath());
        ThreadProperty.set(HS_VAR, testCase.isHistoryServer().toString());
    }

    private Object[][] convertToArray(List<List<TestCase>> list) throws Exception {
        Object[][] result = new Object[list.size()][];
        for(int i = 0; i < list.size(); i++) {
            result[i] = list.get(i).toArray();
        }

        return result;
    }

    @DataProvider(name = "performanceDataProvider")
    public Object[][] dataProvider() throws Exception {

        List<List<TestCase>> result = new ArrayList<>();

        //Check if we have to use an external CSV or we use the default value
        File file = null;

        if(System.getProperty(EXTERNAL_CSV_ENV) != null){
            file = new File(System.getProperty(EXTERNAL_CSV_ENV));
        } else {
            ClassLoader classLoader = getClass().getClassLoader();
            file = new File(classLoader.getResource(DEFAULT_CASES_CSV).getFile());
        }

        List<TestCase> cases = retrieveCases(file);

        for(TestCase testCase : cases) {
            List<TestCase> container = new ArrayList<>();
            container.add(testCase);
            result.add(container);
        }

        logger.info("Found "+cases.size()+ " testCases");

        return convertToArray(result);
    }


    @Test(enabled = true, groups = {"PerformanceTests"}, dataProvider = "performanceDataProvider")
    public void performanceTests(TestCase testCase) throws Exception {
        setExecutionVariables(testCase);
        logger.info("Executing: {}", testCase);
        new CucumberRunner(this.getClass(), FEATURE).runCukes();
    }
}
