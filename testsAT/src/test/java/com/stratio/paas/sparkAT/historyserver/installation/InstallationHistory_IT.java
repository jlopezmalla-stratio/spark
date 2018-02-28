
package com.stratio.paas.sparkAT.historyserver.installation;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = { "src/test/resources/features/historyServerAT/installation.feature" })
public class InstallationHistory_IT extends BaseTest {

    public InstallationHistory_IT() {
    }

    @Test(enabled = true, groups = {"InstallHistoryServer"})
    public void installationHistoryServer() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
