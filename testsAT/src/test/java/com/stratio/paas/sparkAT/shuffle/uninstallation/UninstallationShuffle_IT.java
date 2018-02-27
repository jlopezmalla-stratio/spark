
package com.stratio.paas.sparkAT.shuffle.uninstallation;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/shuffle/uninstallation.feature"
})
public class UninstallationShuffle_IT extends BaseTest {

    public UninstallationShuffle_IT() {
    }

    @Test(enabled = true, groups = {"UninstallShuffle"})
    public void uninstallationSparkShuffle() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
