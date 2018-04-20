
package com.stratio.pf.sparkAT.coverage.installation;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.spark.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/pf/coverage/install-spark-coverage.feature"
})
public class InstallationCoverage_IT extends BaseTest {

    public InstallationCoverage_IT() {
    }

    @Test(enabled = true, groups = {"InstallCoverage"})
    public void installationCoverage() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
