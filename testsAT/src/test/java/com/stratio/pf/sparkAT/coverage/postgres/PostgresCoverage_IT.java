
package com.stratio.pf.sparkAT.coverage.postgres;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.spark.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/pf/coverage/postgres-coverage.feature"
})
public class PostgresCoverage_IT extends BaseTest {

    public PostgresCoverage_IT() {
    }

    @Test(enabled = true, groups = {"PostgresCoverage"})
    public void postgresCoverage() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
