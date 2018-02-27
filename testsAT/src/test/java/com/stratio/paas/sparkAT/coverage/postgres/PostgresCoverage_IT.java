
package com.stratio.paas.sparkAT.coverage.postgres;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/coverage/postgres-coverage.feature"
})
public class PostgresCoverage_IT extends BaseTest {

    public PostgresCoverage_IT() {
    }

    @Test(enabled = true, groups = {"PostgresCoverage"})
    public void postgresCoverage() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
