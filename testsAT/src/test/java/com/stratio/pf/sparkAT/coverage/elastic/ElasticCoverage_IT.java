
package com.stratio.pf.sparkAT.coverage.elastic;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.spark.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/pf/coverage/elastic-coverage.feature"
})
public class ElasticCoverage_IT extends BaseTest {

    public ElasticCoverage_IT() {
    }

    @Test(enabled = true, groups = {"ElasticCoverage"})
    public void elasticCoverage() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
