# Radware ansible workshop example for DefensePro policy creation

**Important notes:**
	*  All Radware ansible modules require installation of vdirect-client.
	Installing it is as simple as running the command: sudo pip install vdirect-client.
    See vdirect_client parameters description [here](https://pypi.python.org/pypi/vdirect-client),
	you may want to modify their defaults while using this playbook. 
	*  Playbook parameters files should be reviewed and modified prior to using the playbook.
	See parameters files description below.


## General
This example contains a playbook folder and a template folder.


**The playbook folder contains:**
1.  dp_policy.yaml playbook file. The playbook uses parameters files from the vars sub folder.
2.  vars sub folder with three playbook parameters files
	*  **vdirect_params.yml** - contains vDirect parameters.
	*  **policy_params.yml** - contains vDirect configuration templates file names, vDirect template instance name,
	   DefensePro device name and all policy parameters needed.


**The template folder contains vDirect configuration template source files:**
1.  dp_version.vm - template for determining DefensePro version 
2.  dp_typedefs.vm - template defining data types used by other templates
3.  dp_crud.vm - template containing macro functions used by other templates
4.  dp_create_policy.vm - template for creating all DefensePro objects related to policy
5.  dp_create_policy.vm - template for removing all DefensePro objects related to policy
  
## Before using the playbook
1.  Review the parameters files under *playbook/vars* subfolder.
    *  In *vdirect_params.yml* file:
       set the vdirect_ip value and other parameters if needed.
       See all vdirect-client parameters description [here](https://pypi.python.org/pypi/vdirect-client)
    *  In *policy_params.yml* file:
       Modify values for each parameter which has no default value.
       Modify default parameters' values, if needed.

## How to use
*  Each of playbook's tasks have a tag
    *  **upload** - for uploading the vDirect configuration templates to vDirect.
    *  **apply** - for running the configuration template which creates the policy in DefensePro.
    *  **remove** - for running the configuration template which removes the policy from DefensePro.

*  You can use the playbook with *upload* tag to just upload the configuration templates.
*  You can use the playbook with *apply* tag to create the policy.
*  You can use the playbook with *remove* tag to remove the policy.
