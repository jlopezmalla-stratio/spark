
package com.stratio.pf.sparkAT.init;

import com.stratio.qa.cucumber.testng.CucumberRunner;
import com.stratio.spark.tests.utils.BaseTest;
import cucumber.api.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(features = {
        "src/test/resources/features/pf/init/download-images.feature"
})
public class DownloadImages_IT extends BaseTest {

    public DownloadImages_IT() {
    }

    @Test(enabled = true, groups = {"DownloadImages"})
    public void downloadImages() throws Exception {
        new CucumberRunner(this.getClass()).runCukes();
    }
}
