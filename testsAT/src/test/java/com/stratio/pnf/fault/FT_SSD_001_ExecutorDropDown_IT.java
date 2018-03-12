
package com.stratio.pnf.fault;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.spark.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/pnf/fault/executors-dropsdown.feature"
})
public class FT_SSD_001_ExecutorDropDown_IT extends BaseTest {

    public FT_SSD_001_ExecutorDropDown_IT() {
    }

    @Test(enabled = true, groups = {"FT_SSD_001_ExecutorDropDown"})
    public void kafkaCoverage() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
