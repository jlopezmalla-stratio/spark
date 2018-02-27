
package com.stratio.paas.sparkAT.historyserver.uninstallation;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/historyServerAT/uninstall.feature"
})
public class UninstallationHistory_IT extends BaseTest {

    public UninstallationHistory_IT() {
    }

    @Test(enabled = true, groups = {"UninstallHistoryServer"})
    public void uninstallationSparkHistoryServer() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
