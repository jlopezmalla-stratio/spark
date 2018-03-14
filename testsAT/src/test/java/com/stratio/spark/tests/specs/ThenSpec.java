
package com.stratio.spark.tests.specs;

import com.stratio.qa.specs.CommonG;
import com.stratio.qa.specs.GivenGSpec;
import com.stratio.qa.specs.WhenGSpec;
import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.java.en.Then;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ThenSpec {

    protected CommonG commonspec;
    protected GivenGSpec givenSpec;
    protected WhenGSpec whenSpec;

    public CommonG getCommonSpec() {
        return this.commonspec;
    }
    public GivenGSpec getCommonGSpec() {return this.givenSpec;}
    public WhenGSpec getWhenGSpec() {return this.whenSpec;}

    public ThenSpec(CommonG spec, GivenGSpec gspec, WhenGSpec whenspec) {

        this.commonspec = spec;
        this.givenSpec = gspec;
        this.whenSpec = whenspec;
    }

    @Then(value = "^I save the value from field in service response '(.*?)' in variable '(.*?)'$")
    public void assertResponseMessage(String jsonField, String envVar) {

        String response = commonspec.getResponse().getResponse();
        commonspec.getLogger().debug("Trying to parse JSON from response into object " + response);

        JSONObject json = new JSONObject(response);

        commonspec.getLogger().debug("JSON object parsed successfuly. Trying to recover key "+jsonField);

        String jsonValue = json.getString(jsonField);
        commonspec.getLogger().debug("Json value retrieved from JSON: "+jsonValue);

        commonspec.getLogger().debug("Saving in envVar "+envVar);

        ThreadProperty.set(envVar, jsonValue);
    }

    public String[] obtainAllNodes() throws Exception{
        commonspec.getLogger().debug("Retrieving nodes from dcos-cli");

        commonspec.runCommandAndGetResult("dcos node | tail -n +2 | cut -d \" \" -f 1 | tr '\\n' ';'");
        String cmdResult = commonspec.getCommandResult();
        commonspec.getLogger().debug("Command result:" + cmdResult);

        //Remove last ; and split
        String[] splitted = cmdResult.substring(0,cmdResult.length()-1).split(";");
        commonspec.getLogger().debug("Nodes found: "+ splitted.length);

        return splitted;
    }

    @Then(value = "^I execute the command '(.*?)' in all the nodes of my cluster with user '(.+?)' and pem '(.+?)'$")
    public void runInAllNodes(String command, String user, String pem) throws Exception{

        String baseFolder = "/tmp";
        String tmpFileBase = baseFolder + "/parallel-dcos-cli-script-steps-";
        String finalFile = tmpFileBase + new Date().getTime() + ".sh";
        String[] nodes = obtainAllNodes();
        StringBuilder finalCmd = new StringBuilder("#!/bin/bash\n");

        commonspec.getLogger().debug("Creating script file:" + finalFile);

        //Prepare script command in parallel
        for(String node : nodes) {
            finalCmd.append(constructSshParallelCmd(command, user, pem, node));
        }

        finalCmd.append("wait");

        //Save the script and send it to the running ssh (dcos-cli) connection and execute it
        BufferedWriter writer = new BufferedWriter(new FileWriter(finalFile));
        writer.write(finalCmd.toString());
        writer.close();

        commonspec.getLogger().debug("Uploading script file:" + finalFile);
        givenSpec.copyToRemoteFile(finalFile, baseFolder);

        //Now launch it
        commonspec.getLogger().debug("Giving permissions:" + finalFile);
        commonspec.getRemoteSSHConnection().runCommand("chmod +x " + finalFile);

        commonspec.getLogger().debug("Executing script file:" + finalFile);
        commonspec.runCommandAndGetResult(finalFile);
    }

    private String constructSshParallelCmd(String command, String user,  String pem, String node) {
        return
                "ssh -o StrictHostKeyChecking=no -o " +
                        "UserKnownHostsFile=/dev/null " +
                        "-i /" + pem + " " +
                        user + "@" + node + " " +
                        command + " &\n";

    }


    @Then(value = "^I clean all the drivers in the dispatcher with name '(.+?)' in dcos host '(.+?)' with mesos master '(.+?)' with user '(.+?)' and password '(.+?)'$")
    public void deleteAllDriversFromDispatcher(String dispatcherId, String dcosHost, String mesosMaster, String user, String password) throws Exception {
        int attempts = 3;
        boolean allkilled = false;
        int waitingTime = 20*1000;

        // Try at most 3 times to kill all the drivers running in the dispatcher
        while(attempts != 0 && !allkilled) {
            commonspec.getLogger().info("Attempt: "+ (3 - attempts + 1));
            commonspec.getLogger().info("Cleaning all drivers from "+dispatcherId+ " in dcosHost: " + dcosHost);

            //Retrieve all the drivers IDs running from Mesos
            List<String> drivers = retrieveDriversRunning(mesosMaster, dispatcherId);

            allkilled = drivers.isEmpty();

            for(String driver: drivers) {
                //Send kills to anyone
                killDriver(driver, dispatcherId, dcosHost, user, password);
            }

            //Wait only if there are drivers to kill
            if(!allkilled) {
                commonspec.getLogger().info("Waiting "+waitingTime+"ms to have the drivers killed");
                Thread.sleep(waitingTime);
            }


            attempts = attempts -1;
        }

        if(!allkilled){
            throw new RuntimeException("Can't kill all the jobs running in the dispatcher after "+attempts+" attempts");
        }

    }

    private List<String> retrieveDriversRunning(String mesosMaster, String dispatcherId) throws Exception {

        String cmd = "curl -s "+mesosMaster+"/frameworks | jq '.frameworks[] | select(.name == \""+dispatcherId+"\") | .tasks | .[] | .id'";
        String privateVar = "___driverIdsRetrieved___";

        commonspec.getLogger().info("Invoking "+cmd+" to retrieve active drivers");
        givenSpec.executeLocalCommand(cmd, null, 0, null, privateVar);

        String found = ThreadProperty.get(privateVar);

        commonspec.getLogger().debug("Command result: "+found);
        List<String> result = new ArrayList<>();

        String[] split = found.split("\"\"");

        for(String driver : split) {
            String driverID = driver.replace("\"", "").trim();
            if(!driverID.isEmpty())
                result.add(driver.replace("\"", "").trim());
        }

        commonspec.getLogger().info("Drivers found: "+ result);

        return result;
    }

    private void killDriver(String driverId, String dispatcherId, String dcosHost, String user, String password) throws Exception {
        //Set SSO cookie
        commonspec.getLogger().info("Setting SSO cookie to invoke kill on dispatcher");
        givenSpec.setGoSecSSOCookie(null, dcosHost, user, password);

        givenSpec.setupRestClient("true", dcosHost, null);

        String endpoint = "/service/"+dispatcherId+"/v1/submissions/kill/"+driverId;

        //Kill it
        commonspec.getLogger().info("Killing driver: "+driverId);
        whenSpec.sendRequestNoDataTable("POST", endpoint, null, null, null, null, null,  "json");
    }
}