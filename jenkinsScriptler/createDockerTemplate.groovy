import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.security.ACL;
import hudson.slaves.*;
import static java.util.Collections.singletonList;
import com.github.kostyasha.yad.commons.*;
import com.github.kostyasha.yad.DockerCloud;
import com.github.kostyasha.yad.DockerContainerLifecycle;
import com.github.kostyasha.yad.DockerSlaveTemplate;
import com.github.kostyasha.yad.launcher.DockerComputerJNLPLauncher;
import com.github.kostyasha.yad.strategy.DockerOnceRetentionStrategy;

// In Jenkins copy/paste this script into the Scriptler interface. 
// Add three parameters:
//		cloudName
//		label
//		image

// Let's find the cloud!
def myCloud = Jenkins.instance.getInstance().getCloud(cloudName);

if (!myCloud) {
  println("Cloud not found, aborting.") 
  return false
}

if (label == "") {
  println("Labels can't be empty strings, aborting.") 
  return false
}

if (image == "") {
  println("Image names can't be empty strings, aborting.")
  return false
}

def launcher = new DockerComputerJNLPLauncher();
launcher.setUser("jenkins");
launcher.setLaunchTimeout(60);

def pullImage = new DockerPullImage();
pullImage.setPullStrategy(DockerImagePullStrategy.PULL_NEVER);

//remove
def removeContainer = new DockerRemoveContainer();
removeContainer.setRemoveVolumes(true);
removeContainer.setForce(true);

def createContainer = new DockerCreateContainer();

//allows Slaves to reference the host Docker to run Docker in Docker
//Inception. Nuff said.
def volumeList = ["/var/run/docker.sock:/var/run/docker.sock"]
createContainer.setVolumes(volumeList);

//lifecycle
def containerLifecycle = new DockerContainerLifecycle();
containerLifecycle.setImage(image);
containerLifecycle.setPullImage(pullImage);
containerLifecycle.setRemoveContainer(removeContainer);
containerLifecycle.setCreateContainer(createContainer);

//Node Properties (environment variables)
def nodeProperties = new ArrayList<>();

def slaveTemplate = new DockerSlaveTemplate();
slaveTemplate.setLabelString(label);
slaveTemplate.setLauncher(launcher);
slaveTemplate.setMode(Node.Mode.EXCLUSIVE);
slaveTemplate.setRetentionStrategy(new DockerOnceRetentionStrategy(5));
slaveTemplate.setDockerContainerLifecycle(containerLifecycle);
slaveTemplate.setNodeProperties(nodeProperties);

def templates = myCloud.getTemplates();
def newTemplates = new ArrayList<DockerSlaveTemplate>();
newTemplates.addAll(templates);
newTemplates.add(slaveTemplate);

myCloud.setTemplates(newTemplates);
Jenkins.getActiveInstance().save();

return true