
package com.stratio.paas.sparkAT.coverage.installation;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/coverage/install-spark-coverage.feature"
})
public class InstallationCoverage_IT extends BaseTest {

    public InstallationCoverage_IT() {
    }

    @Test(enabled = true, groups = {"InstallCoverage"})
    public void installationCoverage() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
