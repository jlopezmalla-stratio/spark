
package com.stratio.pf.sparkAT.shuffle.installation;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.spark.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/pf/shuffle/installation.feature"
})
public class InstallationShuffle_IT extends BaseTest {

    public InstallationShuffle_IT() {
    }

    @Test(enabled = true, groups = {"InstallShuffle"})
    public void installationShuffle() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
