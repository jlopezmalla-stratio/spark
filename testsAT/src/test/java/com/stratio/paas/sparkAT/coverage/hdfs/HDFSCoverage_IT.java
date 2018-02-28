
package com.stratio.paas.sparkAT.coverage.hdfs;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/coverage/hdfs-coverage.feature"
})
public class HDFSCoverage_IT extends BaseTest {

    public HDFSCoverage_IT() {
    }

    @Test(enabled = true, groups = {"HDFSCoverage"})
    public void hdfsCoverage() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
