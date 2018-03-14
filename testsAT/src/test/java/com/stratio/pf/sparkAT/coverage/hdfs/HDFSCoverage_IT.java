
package com.stratio.pf.sparkAT.coverage.hdfs;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.spark.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/pf/coverage/hdfs-coverage.feature"
})
public class HDFSCoverage_IT extends BaseTest {

    public HDFSCoverage_IT() {
    }

    @Test(enabled = true, groups = {"HDFSCoverage"})
    public void hdfsCoverage() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
