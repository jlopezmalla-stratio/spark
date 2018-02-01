
package com.stratio.paas.sparkAT.specs;

import com.stratio.qa.utils.RemoteSSHConnection;
import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import org.json.JSONArray;
import org.json.JSONObject;


import static com.stratio.qa.assertions.Assertions.assertThat;

public class FrameworksSpec extends BaseSpec {

    public FrameworksSpec(Common spec) {
        this.commonspec = spec;
    }

    /*
     * Check value stored in environment variable is higher than value provided
     *
     * @param envVar
     * @param value
     *
     */
    @Then("^value stored in '(.+?)' is higher than '(.+?)'$")
    public void valueHigher(String envVar, String value) throws Exception {
        assertThat(Integer.parseInt(envVar)).isGreaterThan(Integer.parseInt(value));
    }

    /*
     * Check value stored in environment variable is equal to value provided
     *
     * @param envVar
     * @param value
     *
     */
    @Then("^value stored in '(.+?)' is '(.+?)'$")
    public void valueMatches(String envVar, String value) throws Exception {
        assertThat(envVar).matches(value);
    }

    /*
     * Check value stored in environment variable contains value provided
     *
     * @param envVar
     * @param value
     *
     */
    @Then("^value stored in '(.+?)' contains '(.+?)'$")
    public void valueContains(String envVar, String value) throws Exception {
        ThreadProperty.get(envVar).matches(value);
        assertThat(ThreadProperty.get(envVar)).as("Contains "+ value +".").contains(value);
    }

    /*
     * Check value stored in environment variable is different from value provided
     *
     * @param envVar
     * @param value
     *
     */
    @Then("^value stored in '(.+?)' is different from '(.+?)'$")
    public void valueNotEqualTo(String envVar, String value) throws Exception {
        assertThat(envVar).isNotEqualTo(value);
    }

    /*
     * Obtain Mesos Master and store it in environment variable
     *
     * @param clusterMaster
     * @param envVar
     *
     */
    @Then("^I obtain mesos master in cluster '(.+?)' and store it in environment variable '(.+?)'$")
    public void obtainMesosMaster(String clusterMaster, String envVar) throws Exception {
        commonspec.setRemoteSSHConnection(new RemoteSSHConnection("root", "stratio", clusterMaster, null));
        commonspec.getRemoteSSHConnection().runCommand("getent hosts leader.mesos | awk '{print $1}'");
        ThreadProperty.set(envVar, commonspec.getRemoteSSHConnection().getResult().trim());
    }

    /*
     * Obtain Exhibitor user and password and store them in environment variables:
     *    - exhibitorUser
     *    - exhibitorPassword
     *
     * @param dcosCluster
     *
     */
    @Given("^I obtain mesos API user and password in '(.+?)'$")
    public void obtainExhibitorUserPassword(String dcosCluster) throws Exception {
        // Setup ssh connection
        commonspec.setRemoteSSHConnection(new RemoteSSHConnection("root", "stratio", dcosCluster, null));
        // Obtain user
        commonspec.getRemoteSSHConnection().runCommand("cat /opt/mesosphere/packages/exhibitor--*/stratiopaas-exhibitor-realm.properties | awk '{print $1}' | awk '{gsub(/:$/,\"\"); print}'");
        ThreadProperty.set("exhibitorUser", commonspec.getRemoteSSHConnection().getResult().trim());
        // Obtain password
        commonspec.getRemoteSSHConnection().runCommand("cat /opt/mesosphere/packages/exhibitor--*/stratiopaas-exhibitor-realm.properties | awk '{print $2}' | awk '{gsub(/,$/,\"\"); print}'");
        ThreadProperty.set("exhibitorPassword", commonspec.getRemoteSSHConnection().getResult().trim());
    }

    /*
     * Sanitize environment variable
     *
     * @param envVar
     *
     */
    @Then("^I sanitize environment variable '(.+?)'$")
    public void sanitizeEnvVar(String envVar) throws Exception {
        String var = ThreadProperty.get(envVar);
        var = var.trim().substring(1);
        ThreadProperty.set(envVar, var);
    }

    /*
     * Obtain the hostname where a certain process is running
     *
     * @param process
     * @param envVar
     * @param resultEnvVar
     *
     */
    @Given("^I obtain hostname where '(.+?)' is running and id from '(.+?)' and store it in '(.+?)' and '(.+?)'$")
    public void obtainHostname(String process, String envVar, String hostnameEnvVar, String idEnvVar) throws Exception {
        String hostname = null;
        String id = null;

        //String var = ThreadProperty.get(envVar);
        JSONArray jsonArray = new JSONArray(envVar);
        for (int i = 0, size = jsonArray.length(); i < size; i++) {
            JSONObject framework = jsonArray.getJSONObject(i);
            String pid = framework.get("pid").toString();

            // Check if this is the process we are looking for
            if (pid.startsWith(process)) {
                hostname = framework.get("hostname").toString();
                id = framework.get("id").toString();
                break;
            }
        }

        assertThat(hostname).isNotNull();
        assertThat(id).isNotNull();
        ThreadProperty.set(hostnameEnvVar, hostname);
        ThreadProperty.set(idEnvVar, id);
    }

    /*
     * Kill process in host
     *
     * @param process
     * @param type
     * @param hostname
     *
     */
    @Then("^I kill '(.+?)' '(scheduler task|task|app)' in hostname '(.+?)'$")
    public void killProcess(String process, String type, String hostname) throws Exception {
        // Setup ssh connection
        commonspec.setRemoteSSHConnection(new RemoteSSHConnection("root", "stratio", hostname, null));

        // Obtain process id
        switch (type) {
            case "task":
                commonspec.getRemoteSSHConnection().runCommand("ps -ef | grep " + process + " | grep java | grep -v grep | awk '{print $2}' | tail -1");
                break;
            case "scheduler task":
                commonspec.getRemoteSSHConnection().runCommand("ps -ef | grep " + process + " | grep scheduler | grep java | grep -v grep | awk '{print $2}' | tail -1");
                break;
            case "app":
                commonspec.getRemoteSSHConnection().runCommand("ps -ef | grep " + process + " | grep docker | grep -v grep | awk '{print $2}' | tail -1");
                break;
        }
        String processId = commonspec.getRemoteSSHConnection().getResult().trim();
        assertThat(processId).isNotEmpty();

        // Kill process id
        commonspec.getRemoteSSHConnection().runCommand("kill -9 " + processId);
        Thread.sleep(1000);

        // Make sure process is not running
        if ("task".equals(type)) {
            commonspec.getRemoteSSHConnection().runCommand("ps -ef | grep " + process + " | grep java | grep -v grep | awk '{print $2}' | tail -1");
        } else {
            commonspec.getRemoteSSHConnection().runCommand("ps -ef | grep " + process + " | grep scheduler | grep java | grep -v grep | awk '{print $2}' | tail -1");
        }
        processId = commonspec.getRemoteSSHConnection().getResult().trim();
        assertThat(processId).isEmpty();
    }

    /*
     * Obtain info about task running in dcos from dcos cli
     *
     * @param task
     * @param dcosCLI
     * @param hostnameEnvVar
     * @param idEnvVar
     *
     */
    @Given("^I obtain host and id from task '(.+?)' using '(.+?)' and save them in '(.+?)' and '(.+?)'$")
    public void obtainCliTaskInfo(String task, String dcosCliHost, String hostnameEnvVar, String idEnvVar) throws Exception {
         // Setup ssh connection
        commonspec.setRemoteSSHConnection(new RemoteSSHConnection("root", "stratio", dcosCliHost, null));
        // Obtain task host
        commonspec.getRemoteSSHConnection().runCommand("dcos task | grep " + task + " | awk '{print $2}'");
        String host = commonspec.getRemoteSSHConnection().getResult().trim();
        assertThat(host).isNotEmpty();
        // Obtain task id
        commonspec.getRemoteSSHConnection().runCommand("dcos task | grep " + task + " | awk '{print $5}'");
        String id = commonspec.getRemoteSSHConnection().getResult().trim();
        assertThat(id).isNotEmpty();

        // Set environment variables
        ThreadProperty.set(hostnameEnvVar, host);
        ThreadProperty.set(idEnvVar, id);
    }
}
