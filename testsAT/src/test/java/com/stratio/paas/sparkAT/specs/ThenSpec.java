
package com.stratio.paas.sparkAT.specs;

import com.stratio.qa.specs.CommonG;
import com.stratio.qa.specs.GivenGSpec;
import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.java.en.Then;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

public class ThenSpec {

    protected CommonG commonspec;
    protected GivenGSpec givenSpec;

    public CommonG getCommonSpec() {
        return this.commonspec;
    }
    public GivenGSpec getCommonGSpec() {return this.givenSpec;}

    public ThenSpec(CommonG spec, GivenGSpec gspec) {

        this.commonspec = spec;
        this.givenSpec = gspec;
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
}