## SAP Cloud Integration - Partner Directory Manager

<p align="justify" xmlns="http://www.w3.org/1999/html">
This application can be used to configure the partner directory entities of the SAP cloud integration. This app can be
deployed in the SAP BTP foundry as a 
<code>java_buildpack</code>. This application is built using the <a href="https://micronaut.io/">Micronaut framework</a>. 
Micronaut is a modern JVM based full stack development framework for building modular easily microservice and serverless 
applications for the cloud.
</p>
<i>Here are some of the salient feature of this application.</i>
<ul>
<li>Support of full CRUDQ operations on the PD entities.</li>
<li>Ability to manage multiple tenants in the landscape.</li>
<li>Ability to mass download/upload all the entries of a PD entity.</li>
<li>Perform cut-over activity of migrating the PD entries between tenants.</li>
<li>Ability to mass upload data in the PD entities. Only <code>create</code> operation is currently supported.</li>
</ul>

## Pre-requisite:

<ul>
<li>
<p align="justify">
JDK 1.8 or greater installed with JAVA_HOME configured appropriately. This can be checked by the following command: 
<code>java -version</code> in the CMD prompt.
</p>
</li>
</ul>

## Partner directory entities supported:

<ul>
<li>StringParameters</li>
<li>BinaryParameters</li>
<li>AlternativePartners</li>
<li>AuthorizedUsers</li>
</ul>

## How to install this application?

<p align="justify">
<code>git clone https://github.com/deepankarbhowmick/sap_ci_pdm.git</code> or simply download the zip and
extract in your favorite folder. Open intellij and click on open. Browse to the folder where the app has been extracted 
and select the 
<code>build.gradle</code> file. The file needs to be opened as a project. Gradle is the build tool for this app, let 
gradle download all the dependencies required to run the app. This might take few minutes. Once all the dependencies are 
downloaded, the app can be configured.
</p>

## How to configure this application?

<p align="justify">
All the tenants whose partner directory is intended to be managed using this application needs to be configured in the
application. This activity must be completed before deploying the app in the cloud. Of course, we can add new tenants if
needed, but that would require a redeployment of the app in the cloud. The hostname of the tenant management node of all
the intended tenants should be configured in the <code>application.yml</code> file. This file can be found at this 
location: <code>src/main/resources/application.yml</code>. Replace <code>your_tenant_tmn_url</code> with the actual url 
keeping the quotes. Here is a snippet of the <code>application.yml</code> that requires change.
</p>
<table border="0" cellpadding="0" cellspacing="0">
<tr>
<td>
  <code>tenant:</code>
</td>
</tr>
<tr>
<td>
  <code>dev: 'your_tenant_tmn_url'</code>
</td>
</tr>
<tr>
<td>
  <code>qua: 'your_tenant_tmn_url'</code>
</td>
</tr>
<tr>
<td>
  <code>prd: 'your_tenant_tmn_url'</code>
</td>
</tr>  
</table>
<p align="justify">
If required, more tenants can be added. Once the dependencies are downloaded and the tenants are configured, gradle will
be used to build the app making it ready for execution. This can be done by clicking on the 
<code>build</code> gradle-task. After the build has been completed, open the terminal and run the app using command 
<code>gradlew run</code>. Open the link <a href="http://localhost:8080">localhost:8080</a> and check if the app runs 
without any error. If the app opens successfully without any errors, installation has been successful and now the app 
can be deployed to SAP BTP.
</p>

## How to deploy app to SAP BTP?

<ul>
<li>
<p align="justify">
<code>gradle build task</code> created the fat-JAR of the app. This fat-JAR will be deployed to the SAP BTP. Navigate to 
<code>extraction folder\build\libs</code> and check if the <code>sap_pdm-0.1-all.jar</code> file is available.
</li>
<li>
<p align="justify">
We will use the cloud foundry V7 CLI to deploy the app to the BTP cockpit. Download the CLI from 
<a href="https://github.com/cloudfoundry/cli/wiki/V7-CLI-Installation-Guide">SAP CLI Foundry</a> and download the 
appropriate OS installer.
</p>
</li>
<li>
<p align="justify">
Extract the downloaded zip in favorite folder and launch the CLI Installer from the extracted folder.
</p>
</li>
<li>
<p align="justify">
Once installation is successful launch CF CLI from CMD. Validate in cmd prompt
<code>cf version</code>
</p>
<li>
<p align="justify">
Get the API endpoint of the sub-account where we want to deploy the app.
</p>
</li>
<li>
<p align="justify">
Open CLI and set the API endpoint discovered above using the command:
<code>cf api API_endpoint</code>
</p>
</li>
<li>
<p align="justify">
Login in the cloud foundry BTP using the command.
<code>cf login</code>
We will be prompted to enter the username and password.
</p>
</li>
<li>
<p align="justify">
Now we will push the fat-JAR in the BTP using the following command:
<code>cf push app_name -f "path_to_manifest.yml_file"</code> The manifest file could be found in the 
<code>extraction folder</code> The application name (app_name) provided in the <code>Manifest.yml</code>
must match the app_name in the cf push command. Once successfully deployed, navigate to the 
<code>global account > sub account > space > application list</code> and launch the application deployed. 
</p>
</li>
</ul>

## Release History

- [Initial release - July 12, 2021]()