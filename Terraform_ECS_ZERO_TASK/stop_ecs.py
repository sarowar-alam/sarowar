import boto3
import os
from datetime import datetime, timedelta
import logging

# Initialize boto3 clients outside of the handler function
sqs_client = boto3.client('sqs')
ecs_client = boto3.client('ecs')
cloudwatch = boto3.client('cloudwatch')

# Set up logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)


def lambda_handler(event, context):

    current_time = datetime.now()

    active_task_count=0.00
    cpu_utilization = 0.00
    memory_utilization = 0.00
    network_bandwidth=0.00

    # Calculate the start and end times for the metric query (last 5 minutes)
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(seconds=300)

    queue_url = os.environ['queue_url']
    service_name = os.environ['service_name']
    cluster_name = os.environ['cluster_name']


    # Get the approximate number of messages in the SQS queue
    response = sqs_client.get_queue_attributes(QueueUrl=queue_url, AttributeNames=['ApproximateNumberOfMessages'])

    num_messages = int(response['Attributes']['ApproximateNumberOfMessages'])
    logger.info(f"Approximate Number of Messages: {num_messages} TimeStamp: {current_time.strftime("%Y-%m-%d %H:%M:%S")}")
    
    response = ecs_client.describe_services(cluster=cluster_name, services=[service_name])
    if 'services' in response and len(response['services']) > 0:
        service = response['services'][0]
        if 'desiredCount' in service:
            active_task_count = service['desiredCount']

    #Getting ECS CPU Utilization 
    try:
        response = cloudwatch.get_metric_statistics(Namespace='AWS/ECS',MetricName='CPUUtilization',Dimensions=[{'Name': 'ClusterName','Value': cluster_name},{'Name': 'ServiceName','Value': service_name},],StartTime=start_time,EndTime=end_time,Period=300,Statistics=['Average'])
        datapoints = response['Datapoints']
        if datapoints:
            cpu_utilization =  datapoints[-1]['Average'] 
        
        response_memory = cloudwatch.get_metric_statistics(Namespace='AWS/ECS',MetricName='MemoryUtilization',Dimensions=[{'Name': 'ClusterName','Value': cluster_name},{'Name': 'ServiceName','Value': service_name},],StartTime=start_time,EndTime=end_time,Period=300, Statistics=['Average'])
        if 'Datapoints' in response_memory and len(response_memory['Datapoints']) > 0:
            memory_utilization = response_memory['Datapoints'][0]['Average']

        response_network = cloudwatch.get_metric_statistics(Namespace='AWS/ECS',MetricName='NetworkRxBytes',Dimensions=[{'Name': 'ClusterName','Value': cluster_name},{'Name': 'ServiceName','Value': service_name},],StartTime=start_time,EndTime=end_time,Period=300, Statistics=['Average'])
        

        if 'Datapoints' in response_network and len(response_network['Datapoints']) > 0:
            network_bandwidth = response_network['Datapoints'][0]['Average']
            logger.info (network_bandwidth)

        logger.info (f"The ECS Service {service_name} from the Cluster {cluster_name}, CPU Utilization is {cpu_utilization} == Memory {memory_utilization}, Network: {network_bandwidth} TimeStamp: {current_time.strftime("%Y-%m-%d %H:%M:%S")}") 
    except Exception as e:
        logger.info("Error GetMetricStatistics details:", e)

    # Update ECS service desired count based on the number of messages
    if num_messages == 0 and active_task_count > 0:
        try:
            ecs_client.update_service(cluster=cluster_name,service=service_name,desiredCount=0)
            logger.info (f"ECS {service_name} from the cluster {cluster_name} been ShutDown. CPU: {cpu_utilization}, Memory: {memory_utilization}, Network: {network_bandwidth}, TimeStamp: {current_time.strftime("%Y-%m-%d %H:%M:%S")}")
        except Exception as e:
                logger.error(f"Error updating ECS service: {e}")