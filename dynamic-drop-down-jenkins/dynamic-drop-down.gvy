import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import jenkins.model.*

// Jenkins credentials lookup
def creds = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
    StandardUsernamePasswordCredentials.class,
    Jenkins.instance,
    null,
    null
)

def awsCred = creds.find { it.id == '123456-98765-1243-fgrtr-123456789' } 
if (!awsCred) {
    return ["(No AWS credentials found)"]
}

def accessKey = awsCred.username
def secretKey = awsCred.password.getPlainText()

// Full path to AWS CLI
def awsPath = "\"C:\\Program Files\\Amazon\\AWSCLIV2\\aws.exe\""

// Set cluster name (you can loop through multiple if needed)
def cluster = "fargate-container-cluster" 

// Command to get service ARNs
def command = [
    "cmd", "/c",
    "${awsPath} ecs list-services --cluster ${cluster} --output text"
]

// Run with credentials as env
def envVars = [
    "AWS_ACCESS_KEY_ID=${accessKey}",
    "AWS_SECRET_ACCESS_KEY=${secretKey}",
    "AWS_DEFAULT_REGION=us-west-2"
]

def proc = command.execute(envVars, null)

def stdout = new StringBuffer()
def stderr = new StringBuffer()
proc.consumeProcessOutput(stdout, stderr)
def exitCode = proc.waitFor()

if (exitCode != 0) {
    return ["(AWS CLI failed: ${stderr.toString().trim()})"]
}

def result = [:]  // Map for dropdown

stdout.toString().readLines()
    .collectMany { it.tokenize('\t') }
    .findAll { it && it != "SERVICEARNS" && !it.toLowerCase().contains("prod") }  // filter out "prod" if needed
    .collect { arn -> 
        def serviceName = arn.tokenize('/').last()
        [arn, serviceName]
    }
    .sort { a, b -> a[1] <=> b[1] }  // sort by serviceName (A-Z)
    .each { pair ->
        result[pair[0]] = pair[1]  // [arn] = serviceName
    }

return result ?: ["(No ECS services found)"]