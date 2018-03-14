
package com.stratio.pf.sparkAT.coverage.streaminghdfsdynamic;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.spark.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/pf/coverage/streaming-hdfs-dynamic.feature"
})
public class StreamingHDFSDynamicCoverage_IT extends BaseTest {

    public StreamingHDFSDynamicCoverage_IT() {
    }

    @Test(enabled = true, groups = {"DynamicCoverage"})
    public void streamingHDFSDynamicCoverage() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
