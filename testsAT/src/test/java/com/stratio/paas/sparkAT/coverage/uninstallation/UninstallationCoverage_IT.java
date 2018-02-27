
package com.stratio.paas.sparkAT.coverage.uninstallation;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/coverage/uninstall-spark-coverage.feature"
})
public class UninstallationCoverage_IT extends BaseTest {

    public UninstallationCoverage_IT() {
    }

    @Test(enabled = true, groups = {"UninstallCoverage"})
    public void uninstallationSparkCoverage() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
