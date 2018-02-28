
package com.stratio.paas.sparkAT.dispatcher.uninstallation;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/dispatcherAT/uninstall.feature"
})
public class UninstallationDispatcher_IT extends BaseTest {

    public UninstallationDispatcher_IT() {
    }

    @Test(enabled = true, groups = {"UninstallDispatcher"})
    public void uninstallationSparkDispatcher() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
