# Radware ansible workshop example for CICD BlueGreen

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

## General
This example contains a playbook folder and a workflow folder.

**The playbook folder contains:**
1.  cicd_bluegreen.yaml playbook file. The playbook uses parameters files from the vars folder.
2.  vars folder with three playbook parameters files
	*  **vdirect_params.yml** - contains vDirect parameters.
	*  **bluegreen_params.yml** - contains vDirect workflow template file name, vDirect workflow instance name,
	   Alteon device name and application which blue-green CICD will be performed on.


**The workflow folder contains:**
1.  vDirect workflow source files. 
  
## Before using the playbook
1.  First, the vDirect workflow should be created from the source.
    Zip the content of the *workflow* folder as a ZIP file.
    The name of the ZIP file will be used as a value for *workflow_template_file* parameter
    in *bluegreen_params.yml* file. The default file name is *cicd_bluegreen.zip*
	**Example:**
	```
	cd worklow
	zip -r cicd_bluegreen.zip *
	```
2.  Review the parameters files under *playbook/vars* subfolder.
    *  In *vdirect_params.yml* file:
       set the vdirect_ip value and other parameters if needed.
       See all vdirect-client parameters description [here](https://pypi.python.org/pypi/vdirect-client)
    *  In *bluegreen_params.yml* file:
       Modify *workflow_template_file* parameter value if needed.
       Modify *device* and *application* parameters values.

## How to use

*  Each of playbook's tasks have a tag
    *  **upload** - for uploading the vDirect workflow template ZIP file to vDirect.
       The default workflow template name is *BlueGreen CICD*.
    *  **create** - for creating the workflow instance from the workflow template.
       The workflow name will be *BlueGreen CICD*.
    *  **green** - switch the application to green servers group
       **Note:** after switching to green servers groups, the only allowed workflow 
                 action is *blue*
    *  **blue** - switch the application to blue servers groups
       **Note:** after switching to blue servers group, the only allowed workflow 
                 action is *green*
    *  **remove** - removing the *BlueGreen CICD* workflow.

*  You can use the playbook with *upload* and *create* tags.
   It will upload the workflow template and create the workflow.
	**Example:**
	```
	ansible-playbook cicd_bluegreen.yaml --tags upload,create
	```

*  You can use the playbook with *green* tag to to switch the application to green servers groups.
	**Example:**
	```
	ansible-playbook cicd_bluegreen.yaml --tags green
	```
*  You can use the playbook with *blue* tag to to switch the application to blue servers groups.
	**Example:**
	```
	ansible-playbook cicd_bluegreen.yaml --tags blue
	```
*  You can use the playbook with *remove* tag to remove the vDirect workflow.
	**Example:**
	```
	ansible-playbook cicd_bluegreen.yaml --tags remove
	```