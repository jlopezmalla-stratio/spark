
package com.stratio.paas.sparkAT.shuffle.installation;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/shuffle/installation.feature"
})
public class InstallationShuffle_IT extends BaseTest {

    public InstallationShuffle_IT() {
    }

    @Test(enabled = true, groups = {"InstallShuffle"})
    public void installationShuffle() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
