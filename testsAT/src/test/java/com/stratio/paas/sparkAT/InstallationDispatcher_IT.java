
package com.stratio.paas.sparkAT;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = { "src/test/resources/features/dispatcherAT/installation.feature" })
public class InstallationDispatcher_IT extends BaseTest {

    public InstallationDispatcher_IT() {
    }

    @Test(enabled = true, groups = {"installation"})
    public void installation() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
