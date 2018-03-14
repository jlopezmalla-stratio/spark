
package com.stratio.pf.sparkAT.dispatcher.uninstallation;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.spark.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/pf/dispatcherAT/uninstall.feature"
})
public class UninstallationDispatcher_IT extends BaseTest {

    public UninstallationDispatcher_IT() {
    }

    @Test(enabled = true, groups = {"UninstallDispatcher"})
    public void uninstallationSparkDispatcher() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
