
package com.stratio.pnf.fault;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.spark.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/pnf/fault/dispatcher-dropsdown.feature"
})
public class FT_SSD_002_DispatcherDropDown_IT extends BaseTest {

    public FT_SSD_002_DispatcherDropDown_IT() {
    }

    @Test(enabled = true, groups = {"FT_SSD_002_DispatcherDropDown"})
    public void dispatcherDropDownCoverage() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
