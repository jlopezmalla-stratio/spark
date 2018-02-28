
package com.stratio.paas.sparkAT.coverage.kafka;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/coverage/kafka-coverage.feature"
})
public class KafkaCoverage_IT extends BaseTest {

    public KafkaCoverage_IT() {
    }

    @Test(enabled = true, groups = {"KafkaCoverage"})
    public void kafkaCoverage() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
