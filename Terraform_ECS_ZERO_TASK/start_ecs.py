import boto3
import os
import time
from datetime import datetime

def lambda_handler(event, context):
    sqs_client = boto3.client('sqs')
    ecs_client = boto3.client('ecs')

    # Get environment variables
    queue_url = os.environ['queue_url']
    service_name = os.environ['service_name']
    cluster_name = os.environ['cluster_name']    

    # Get the Queue Name
    name_start_index = queue_url.rfind('/') + 1  # Find the index of the last '/'
    queue_name = queue_url[name_start_index:]

    for i in range(4):  # Run the logic 3 times
        current_time = datetime.now()
        active_task_count = 0

        try:
            response = sqs_client.get_queue_attributes(QueueUrl=queue_url, AttributeNames=['ApproximateNumberOfMessages'])
            num_messages = int(response['Attributes']['ApproximateNumberOfMessages'])
            print(f"SQS {queue_name} Number of Messages: {num_messages} TimeStamp: {current_time.strftime('%Y-%m-%d %H:%M:%S')}")
        except Exception as e:
            print("Error get_queue_attributes:", e)

        # Update ECS service desired count based on the number of messages
        if num_messages > 0:
            try:
                response = ecs_client.describe_services(cluster=cluster_name, services=[service_name])
            except Exception as e:
                print("Error describe_services:", e)

            if 'services' in response and len(response['services']) > 0:
                service = response['services'][0]
                if 'desiredCount' in service:
                    active_task_count = service['desiredCount']
                    if active_task_count == 0:
                        try:
                            ecs_client.update_service(cluster=cluster_name, service=service_name, desiredCount=1)
                            print(f"************* ECS {service_name} from the cluster {cluster_name} updated with 1 Task. TimeStamp: {current_time.strftime('%Y-%m-%d %H:%M:%S')}")
                        except Exception as e:
                            print("Error update_service:", e)
        
        # Wait 15 seconds before the next iteration, unless it's the last run
        if i < 3:
            time.sleep(15)

