
package com.stratio.pf.sparkAT.coverage.kafka;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.spark.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/pf/coverage/kafka-coverage.feature"
})
public class KafkaCoverage_IT extends BaseTest {

    public KafkaCoverage_IT() {
    }

    @Test(enabled = true, groups = {"KafkaCoverage"})
    public void kafkaCoverage() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
