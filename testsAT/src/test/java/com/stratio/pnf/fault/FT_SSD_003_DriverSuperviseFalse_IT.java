
package com.stratio.pnf.fault;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.spark.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/pnf/fault/driver-supervise-false.feature"
})
public class FT_SSD_003_DriverSuperviseFalse_IT extends BaseTest {

    public FT_SSD_003_DriverSuperviseFalse_IT() {
    }

    @Test(enabled = true, groups = {"FT_SSD_003_DriverSuperviseFalse"})
    public void driverSuperviseFalseCoverage() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
