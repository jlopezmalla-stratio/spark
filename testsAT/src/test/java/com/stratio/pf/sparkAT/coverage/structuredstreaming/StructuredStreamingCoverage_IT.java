
package com.stratio.pf.sparkAT.coverage.structuredstreaming;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.spark.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/pf/coverage/structured-streaming-coverage.feature"
})
public class StructuredStreamingCoverage_IT extends BaseTest {

    public StructuredStreamingCoverage_IT() {
    }

    @Test(enabled = true, groups = {"StructuredStreamingCoverage"})
    public void structuredStreamingCoverage() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
