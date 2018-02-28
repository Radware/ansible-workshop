# Radware ansible workshop example for CICD Canary

**Important notes:**
	*  All Radware ansible modules require installation of vdirect-client.
	Installing it is as simple as running the command: sudo pip install vdirect-client.
    See vdirect_client parameters description [here](https://pypi.python.org/pypi/vdirect-client),
	you may want to modify their defaults while using this playbook. 
	*  Playbook parameters files should be reviewed and modified prior to using the playbook.
	See parameters files description below.


## Installation
In order to use this workshop you need a linux server with GIT, ANSIBLE v2.5 and ZIP installed.

1.  Create a folder for cloning this GIT repositorty.
2.  Clone the repository
```
git clone https://github.com/Radware/ansible-workshop.git
```
3. Change directory to the CICD canary workshop example
```
cd ansible-workshop/cicd/canary
```

## General
This example contains this **README.md** file, **playbook** folder and **workflow** folder.

**The playbook folder contains:**
1.  cicd_canary.yaml playbook file. The playbook uses parameters files from the vars folder.
2.  vars folder with three playbook parameters files
	*  **vdirect_params.yml** - contains vDirect parameters.
	*  **canary_params.yml** - contains vDirect workflow template file name, vDirect workflow instance name,
	   Alteon device name, real servers group name the canary server will be added to,
	   and canary real server parameters.
	*  **weight_params.yml** - contains real server weight values, for group existing servers and for the canary server.


**The workflow folder contains:**
1.  vDirect workflow source files. 

  
## Before using the playbook
1.  First, the vDirect workflow should be created from the source.
    Zip the content of the *workflow* folder as a ZIP file.
    The name of the ZIP file will be used as a value for *workflow_template_file* parameter
    in *canary_params.yml* file. The default file name is *cicd_canary.zip*
	**Example:**
	```
	cd worklow
	zip -r cicd_canary.zip *
	```
2.  Review the parameters files under *playbook/vars* subfolder.
    *  In *vdirect_params.yml* file:
       set the vdirect_ip value and other parameters if needed.
       See all vdirect-client parameters description [here](https://pypi.python.org/pypi/vdirect-client)
    *  In *canary_params.yml* file:
       Modify *workflow_template_file* parameter value if needed.
       Modify *device*, *group* and *canary_server_address* parameters values,
       and others if needed.
    *  In *weight_params.yml* file, modify servers' weigh values if needed.

## How to use

*  Each of playbook's tasks have a tag
    *  **upload** - for uploading the vDirect workflow template ZIP file to vDirect.
       The default workflow template name is *Canary CICD*.
    *  **create** - for creating the workflow instance from the workflow template.
       The workflow name will be *Canary CICD*.
    *  **deploy** - deploying the canary server
       **Note:** after deploying the canary server, the only allowed workflow 
                 actions are *undeploy* and *update*
    *  **update** - updating real servers' weight
    *  **undeploy** - undeploying the canary server
    *  **remove** - removing the *Canary CICD* workflow. The canary server will be undeployed.

*  You can use the playbook with *upload* and *create* tags.
   It will upload the workflow template, create the workflow and will not deploy the canary server.
	**Example:**
	```
	ansible-playbook cicd_canary.yaml --tags upload,create
	```

*  You can use the playbook with *update* tag to just update servers weights.
	**Example:**
	```
	ansible-playbook cicd_canary.yaml --tags update
	```
   **Note:** You should change weight values in parameters file first.
*  You can use the playbook with *undeploy* tag to undeploy the canary server.
	**Example:**
	```
	ansible-playbook cicd_canary.yaml --tags undeploy
	```
*  You can use the playbook with *deploy* tag to deploy the canary server.
	**Example:**
	```
	ansible-playbook cicd_canary.yaml --tags deploy
	```
*  You can use the playbook with *remove* tag to remove the vDirect workflow.
   The canary server will be undeployed automatically in this case.
	**Example:**
	```
	ansible-playbook cicd_canary.yaml --tags remove
	```