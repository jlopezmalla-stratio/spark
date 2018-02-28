
package com.stratio.paas.sparkAT.coverage.streaminghdfsdynamic;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/coverage/streaming-hdfs-dynamic.feature"
})
public class StreamingHDFSDynamicCoverage_IT extends BaseTest {

    public StreamingHDFSDynamicCoverage_IT() {
    }

    @Test(enabled = true, groups = {"DynamicCoverage"})
    public void streamingHDFSDynamicCoverage() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
