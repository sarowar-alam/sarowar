import boto3
import json
import os
from datetime import datetime, timedelta
from botocore.exceptions import ClientError

def assume_role(role_arn, session_name, duration_seconds=3600, validate_credentials=True):
    """Assume IAM role and return credentials"""
    try:
        sts_client = boto3.client('sts')
        response = sts_client.assume_role(
            RoleArn=role_arn,
            RoleSessionName=session_name,
            DurationSeconds=duration_seconds
        )
        return response['Credentials']
    except Exception as e:
        print(f"Failed to assume role {role_arn}: {e}")
        raise

def send_email_SES(html_body, ses_role_arn, instance_id):
    """Send email using SES with assumed role"""
    try:
        # Assume role for SES
        credentials_ses = assume_role(
            ses_role_arn,
            session_name="SESSendEmailSession",
            duration_seconds=3600
        )
        
        AWS_REGION = 'us-east-1'
        SENDER_EMAIL = 'DevOps_Automation <noreply@your_company.com>'
        RECIPIENT_EMAIL = ['your_name@your_company.com']
        SUBJECT = f'[Automation] EC2 Found idle in AWS - Instance {instance_id}'
        
        ses_client = boto3.client(
            'ses',
            region_name=AWS_REGION,
            aws_access_key_id=credentials_ses['AccessKeyId'],
            aws_secret_access_key=credentials_ses['SecretAccessKey'],
            aws_session_token=credentials_ses['SessionToken']
        )

        response = ses_client.send_email(
            Destination={'ToAddresses': RECIPIENT_EMAIL},
            Message={
                'Body': { 
                    'Html': {
                        'Charset': 'UTF-8',
                        'Data': html_body,
                    }
                },
                'Subject': {
                    'Charset': 'UTF-8',
                    'Data': SUBJECT,
                },
            },
            Source=SENDER_EMAIL,
        )
        print("Email sent! Message ID:", response['MessageId'])
        return True
    except Exception as e:
        print("Email sending failed:", e)
        return False

def format_bytes_to_readable(value_bytes):
    """Convert bytes to human readable format"""
    if value_bytes >= 1024 * 1024:
        return f"{value_bytes/(1024*1024):.1f} MB"
    elif value_bytes >= 1024:
        return f"{value_bytes/1024:.1f} KB"
    else:
        return f"{value_bytes:.0f} Bytes"

class EC2AutoShutdown:
    def __init__(self, monitoring_role_arn, ses_role_arn, region='us-west-2'):
        self.region = region
        self.monitoring_role_arn = monitoring_role_arn
        self.ses_role_arn = ses_role_arn
        self.ec2_client = None
        self.cloudwatch_client = None
        self.setup_clients()
        
    def setup_clients(self):
        """Setup AWS clients with assumed role"""
        credentials = assume_role(
            self.monitoring_role_arn, 
            "EC2MonitoringSession"
        )
        
        self.ec2_client = boto3.client(
            'ec2',
            aws_access_key_id=credentials['AccessKeyId'],
            aws_secret_access_key=credentials['SecretAccessKey'],
            aws_session_token=credentials['SessionToken'],
            region_name=self.region
        )
        
        self.cloudwatch_client = boto3.client(
            'cloudwatch',
            aws_access_key_id=credentials['AccessKeyId'],
            aws_secret_access_key=credentials['SecretAccessKey'],
            aws_session_token=credentials['SessionToken'],
            region_name=self.region
        )
    
    def get_instances_by_tags(self, tags):
        """Get instances matching specific tags"""
        filters = [
            {'Name': 'instance-state-name', 'Values': ['running']}
        ]
        
        for key, value in tags.items():
            filters.append({'Name': f'tag:{key}', 'Values': [value]})
        
        try:
            response = self.ec2_client.describe_instances(Filters=filters)
            instances = []
            
            for reservation in response['Reservations']:
                for instance in reservation['Instances']:
                    # Get EBS volume IDs for this instance
                    volume_ids = []
                    for block_device in instance.get('BlockDeviceMappings', []):
                        if 'Ebs' in block_device:
                            volume_ids.append(block_device['Ebs']['VolumeId'])
                    
                    instance_info = {
                        'InstanceId': instance['InstanceId'],
                        'InstanceType': instance['InstanceType'],
                        'LaunchTime': instance['LaunchTime'],
                        'Name': self.get_instance_name(instance),
                        'Tags': instance.get('Tags', []),
                        'VolumeIds': volume_ids
                    }
                    instances.append(instance_info)
            
            return instances
        except Exception as e:
            print(f"Error getting instances by tags: {e}")
            return []
    
    def get_instance_name(self, instance):
        """Extract instance name from tags"""
        for tag in instance.get('Tags', []):
            if tag['Key'] == 'Name':
                return tag['Value']
        return 'No-Name'
    
    def calculate_average_from_datapoints(self, datapoints):
        """Calculate average from CloudWatch datapoints"""
        if not datapoints:
            return None
        
        datapoints.sort(key=lambda x: x['Timestamp'])
        last_hour_points = datapoints[-12:] if len(datapoints) >= 12 else datapoints
        
        total = sum(point.get('Sum', 0) for point in last_hour_points)
        return total / len(last_hour_points)
    
    def check_ebs_metrics_detailed(self, volume_ids):
        """Check EBS volume metrics with per-volume details - ANY volume active approach"""
        if not volume_ids:
            return {
                'status': 'NO_VOLUMES', 
                'value': 0, 
                'is_low': True,
                'volume_details': [],
                'any_volume_active': False,
                'active_volumes_count': 0
            }
        
        total_read_bytes = 0
        total_write_bytes = 0
        volumes_with_data = 0
        volume_details = []
        any_volume_active = False
        active_volumes_count = 0
        
        for volume_id in volume_ids:
            try:
                # Check VolumeReadBytes
                read_response = self.cloudwatch_client.get_metric_statistics(
                    Namespace='AWS/EBS',
                    MetricName='VolumeReadBytes',
                    Dimensions=[{'Name': 'VolumeId', 'Value': volume_id}],
                    StartTime=datetime.utcnow() - timedelta(minutes=70),
                    EndTime=datetime.utcnow(),
                    Period=300,
                    Statistics=['Sum']
                )
                
                # Check VolumeWriteBytes
                write_response = self.cloudwatch_client.get_metric_statistics(
                    Namespace='AWS/EBS',
                    MetricName='VolumeWriteBytes',
                    Dimensions=[{'Name': 'VolumeId', 'Value': volume_id}],
                    StartTime=datetime.utcnow() - timedelta(minutes=70),
                    EndTime=datetime.utcnow(),
                    Period=300,
                    Statistics=['Sum']
                )
                
                # Calculate averages for this volume
                read_avg = self.calculate_average_from_datapoints(read_response['Datapoints'])
                write_avg = self.calculate_average_from_datapoints(write_response['Datapoints'])
                
                read_avg = read_avg if read_avg else 0
                write_avg = write_avg if write_avg else 0
                
                volume_activity = read_avg + write_avg
                
                # ANY VOLUME ACTIVE APPROACH: Consider volume active if > 100 KB/s
                volume_is_active = volume_activity > 102400  # 100 KB threshold
                
                if volume_is_active:
                    any_volume_active = True
                    active_volumes_count += 1
                
                volume_info = {
                    'volume_id': volume_id,
                    'read_bytes': read_avg,
                    'write_bytes': write_avg,
                    'total_activity': volume_activity,
                    'is_active': volume_is_active,
                    'has_data': read_avg > 0 or write_avg > 0
                }
                volume_details.append(volume_info)
                
                # Add to totals for overall calculation (for display only)
                total_read_bytes += read_avg
                total_write_bytes += write_avg
                
                if volume_info['has_data']:
                    volumes_with_data += 1
                    
            except Exception as e:
                print(f"Error checking EBS metrics for volume {volume_id}: {e}")
                # Mark volume as error but continue with others
                volume_details.append({
                    'volume_id': volume_id,
                    'read_bytes': 0,
                    'write_bytes': 0,
                    'total_activity': 0,
                    'is_active': False,
                    'has_data': False,
                    'error': str(e)
                })
                continue
        
        # Calculate overall activity (for display purposes)
        total_activity = total_read_bytes + total_write_bytes
        avg_activity_per_volume = total_activity / len(volume_ids) if volume_ids else 0
        
        # ANY VOLUME ACTIVE APPROACH: Disk activity is LOW only if NO volumes are active
        is_low_overall = not any_volume_active
        
        return {
            'status': 'LOW' if is_low_overall else 'HIGH',
            'value': avg_activity_per_volume,
            'is_low': is_low_overall,
            'read_bytes': total_read_bytes,
            'write_bytes': total_write_bytes,
            'volume_count': len(volume_ids),
            'volumes_with_data': volumes_with_data,
            'any_volume_active': any_volume_active,
            'active_volumes_count': active_volumes_count,
            'volume_details': volume_details
        }
    
    def check_instance_metrics_with_details(self, instance_id, volume_ids):
        """Check instance metrics and return detailed results"""
        end_time = datetime.utcnow()
        start_time = end_time - timedelta(minutes=70)
        
        metrics_config = {
            'CPUUtilization': {'threshold': 3.0, 'unit': '%'},
            'NetworkIn': {'threshold': 10000, 'unit': 'Bytes'},
            'NetworkOut': {'threshold': 10000, 'unit': 'Bytes'},
        }
        
        metric_results = {}
        low_activity_count = 0
        
        # Check standard EC2 metrics
        for metric_name, config in metrics_config.items():
            try:
                response = self.cloudwatch_client.get_metric_statistics(
                    Namespace='AWS/EC2',
                    MetricName=metric_name,
                    Dimensions=[{'Name': 'InstanceId', 'Value': instance_id}],
                    StartTime=start_time,
                    EndTime=end_time,
                    Period=300,
                    Statistics=['Average']
                )
                
                datapoints = response['Datapoints']
                
                if not datapoints:
                    metric_results[metric_name] = {
                        'status': 'NO_DATA',
                        'value': None,
                        'is_low': True
                    }
                    low_activity_count += 1
                    continue
                
                datapoints.sort(key=lambda x: x['Timestamp'])
                last_hour_points = datapoints[-12:] if len(datapoints) >= 12 else datapoints
                
                total = sum(point['Average'] for point in last_hour_points)
                average = total / len(last_hour_points)
                
                is_low = average < config['threshold']
                
                metric_results[metric_name] = {
                    'status': 'LOW' if is_low else 'HIGH',
                    'value': average,
                    'is_low': is_low
                }
                
                if is_low:
                    low_activity_count += 1
                    
            except Exception as e:
                print(f"Error checking {metric_name} for {instance_id}: {e}")
                metric_results[metric_name] = {
                    'status': 'ERROR',
                    'value': None,
                    'is_low': True
                }
                low_activity_count += 1
                continue
        
        # Check EBS metrics for disk activity with ANY VOLUME ACTIVE approach
        ebs_result = self.check_ebs_metrics_detailed(volume_ids)
        metric_results['EBSDiskActivity'] = ebs_result
        
        # ANY VOLUME ACTIVE APPROACH: Only count as low activity if NO volumes are active
        if not ebs_result['any_volume_active']:
            low_activity_count += 1
            print(f"  - EBS Activity: All {ebs_result['volume_count']} volumes inactive")
        else:
            print(f"  - EBS Activity: {ebs_result['active_volumes_count']} volume(s) active")
        
        return metric_results, low_activity_count
    
    def print_metric_details(self, instance_id, metric_results):
        """Print detailed metric information with multi-volume support"""
        print(f"Detailed metrics for {instance_id}:")
        
        for metric_name, result in metric_results.items():
            if metric_name == 'CPUUtilization':
                threshold = 3.0
                threshold_display = f"{threshold} %"
                if result['status'] == 'NO_DATA':
                    print(f"  {metric_name}: NO DATA (assuming inactive)")
                elif result['status'] == 'ERROR':
                    print(f"  {metric_name}: ERROR (assuming inactive)")
                else:
                    value_display = f"{result['value']:.2f} %"
                    status = "LOW" if result['is_low'] else "HIGH"
                    print(f"  {status} {metric_name}: {value_display} (threshold: {threshold_display})")
            
            elif metric_name in ['NetworkIn', 'NetworkOut']:
                threshold = 10000
                threshold_display = f"{threshold} Bytes ({format_bytes_to_readable(threshold)})"
                
                if result['status'] == 'NO_DATA':
                    print(f"  {metric_name}: NO DATA (assuming inactive)")
                elif result['status'] == 'ERROR':
                    print(f"  {metric_name}: ERROR (assuming inactive)")
                else:
                    value_display = format_bytes_to_readable(result['value'])
                    status = "LOW" if result['is_low'] else "HIGH"
                    print(f"  {status} {metric_name}: {value_display} (threshold: {threshold_display})")
            
            elif metric_name == 'EBSDiskActivity':
                threshold = 102400
                threshold_display = f"{threshold} Bytes ({format_bytes_to_readable(threshold)})"
                
                if result['status'] == 'NO_DATA':
                    print(f"  EBS Disk Activity: NO DATA across {result['volume_count']} volumes")
                elif result['status'] == 'NO_VOLUMES':
                    print(f"  EBS Disk Activity: NO EBS VOLUMES")
                elif result['status'] == 'ERROR':
                    print(f"  EBS Disk Activity: ERROR")
                else:
                    value_display = format_bytes_to_readable(result['value'])
                    status = "LOW" if result['is_low'] else "HIGH"
                    read_display = format_bytes_to_readable(result['read_bytes'])
                    write_display = format_bytes_to_readable(result['write_bytes'])
                    
                    print(f"  {status} EBS Disk Activity: {value_display} avg (threshold: {threshold_display})")
                    print(f"    - Total Read: {read_display}, Total Write: {write_display}")
                    print(f"    - Active Volumes: {result['active_volumes_count']}/{result['volume_count']}")
                    
                    # Show per-volume details for active volumes
                    active_volumes = [v for v in result.get('volume_details', []) if v['is_active']]
                    if active_volumes:
                        print(f"    - Active Volume Details:")
                        for vol in active_volumes:
                            vol_activity = format_bytes_to_readable(vol['total_activity'])
                            read_vol = format_bytes_to_readable(vol['read_bytes'])
                            write_vol = format_bytes_to_readable(vol['write_bytes'])
                            print(f"      • {vol['volume_id'][:12]}...: {vol_activity} (R: {read_vol}, W: {write_vol})")
    
    def is_instance_unused(self, instance_id, volume_ids):
        """Check if instance is unused based on metrics with detailed logging"""
        metric_results, low_activity_count = self.check_instance_metrics_with_details(instance_id, volume_ids)
        self.print_metric_details(instance_id, metric_results)
        
        # We now have 4 metrics: CPU, NetworkIn, NetworkOut, EBSDiskActivity
        # Consider unused if 3 out of 4 metrics show low activity
        # With ANY VOLUME ACTIVE approach, EBSDiskActivity is low only if NO volumes are active
        return low_activity_count >= 3
    
    def send_warning_email(self, instance_id, instance_name):
        """Send warning email for idle instance"""
        html_body = f"""
        <html>
        <head>
            <style>
                body {{ font-family: Arial, sans-serif; }}
                .header {{ color: #ff6600; font-size: 18px; font-weight: bold; }}
                .info {{ background-color: #f5f5f5; padding: 15px; border-radius: 5px; }}
                .warning {{ color: #cc0000; font-weight: bold; }}
            </style>
        </head>
        <body>
            <div class="header">🚨 EC2 Instance Auto-Stop Warning</div>
            <p>Your EC2 instance has been detected as inactive and will be automatically stopped in 15 minutes.</p>
            
            <div class="info">
                <strong>Instance Details:</strong><br>
                • Name: {instance_name}<br>
                • ID: {instance_id}<br>
                • Region: {self.region}<br>
                • Detection Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}<br>
                • Scheduled Stop: {(datetime.now() + timedelta(minutes=15)).strftime('%Y-%m-%d %H:%M:%S')}
            </div>
            
            <p class="warning">⚠️ To prevent shutdown, simply use the instance (SSH/RDP or run commands) to generate activity.</p>
            
            <p>
                <strong>AWS Console:</strong><br>
                <a href="https://{self.region}.console.aws.amazon.com/ec2/home?region={self.region}#InstanceDetails:instanceId={instance_id}">
                    View Instance in AWS Console
                </a>
            </p>
            
            <p>This is an automated message from EC2 Auto-Shutdown System.</p>
        </body>
        </html>
        """
        
        return send_email_SES(html_body, self.ses_role_arn, instance_id)
    
    def send_shutdown_email(self, instance_id, instance_name):
        """Send shutdown notification email"""
        html_body = f"""
        <html>
        <head>
            <style>
                body {{ font-family: Arial, sans-serif; }}
                .header {{ color: #cc0000; font-size: 18px; font-weight: bold; }}
                .info {{ background-color: #f5f5f5; padding: 15px; border-radius: 5px; }}
            </style>
        </head>
        <body>
            <div class="header">🛑 EC2 Instance Stopped</div>
            <p>Your EC2 instance has been stopped due to inactivity.</p>
            
            <div class="info">
                <strong>Instance Details:</strong><br>
                • Name: {instance_name}<br>
                • ID: {instance_id}<br>
                • Region: {self.region}<br>
                • Stop Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
            </div>
            
            <p>The instance was stopped after remaining inactive for 75+ minutes total.</p>
            <p>You can start the instance again when needed from the AWS Console.</p>
            
            <p>
                <strong>AWS Console:</strong><br>
                <a href="https://{self.region}.console.aws.amazon.com/ec2/home?region={self.region}#InstanceDetails:instanceId={instance_id}">
                    View Instance in AWS Console
                </a>
            </p>
            
            <p>This is an automated message from EC2 Auto-Shutdown System.</p>
        </body>
        </html>
        """
        
        return send_email_SES(html_body, self.ses_role_arn, instance_id)
    
    def stop_instance(self, instance_id, instance_name):
        """Stop EC2 instance"""
        try:
            self.ec2_client.stop_instances(InstanceIds=[instance_id])
            print(f"Successfully stopped instance: {instance_name} ({instance_id})")
            return True
            
        except Exception as e:
            print(f"Failed to stop instance {instance_id}: {e}")
            return False

def lambda_handler(event, context):
    """
    Main Lambda handler function
    """
    print("EC2 Auto Shutdown Lambda Started")
    
    # Configuration from environment variables
    MONITORING_ROLE_ARN = os.environ['MONITORING_ROLE_ARN']
    SES_ROLE_ARN = os.environ['SES_ROLE_ARN']
    TARGET_REGION = os.environ.get('TARGET_REGION', 'us-west-2')
    
    # Parse tags from environment variable
    tags_json = os.environ.get('TAGS_FOR_MATCHING', '{"Environment": "Mainline", "System": "Xbox"}')
    TARGET_TAGS = json.loads(tags_json)
    
    # Initialize the automator
    automator = EC2AutoShutdown(MONITORING_ROLE_ARN, SES_ROLE_ARN, TARGET_REGION)
    
    # Get target instances
    instances = automator.get_instances_by_tags(TARGET_TAGS)
    print(f"Found {len(instances)} running instances with tags {TARGET_TAGS}")
    
    results = {
        'checked_instances': len(instances),
        'warnings_sent': 0,
        'instances_stopped': 0,
        'errors': []
    }
    
    # Use DynamoDB for state tracking
    state_table_name = 'ec2-auto-shutdown-state'
    dynamodb = boto3.resource('dynamodb')
    
    try:
        table = dynamodb.Table(state_table_name)
    except Exception as e:
        print(f"DynamoDB table error: {e}")
        # Fallback to in-memory state (for testing)
        state_data = {}
    
    for instance in instances:
        instance_id = instance['InstanceId']
        instance_name = instance['Name']
        volume_ids = instance.get('VolumeIds', [])
        
        print(f"Checking instance: {instance_name} ({instance_id})")
        print(f"EBS Volumes: {volume_ids}")
        
        try:
            # Check metrics and get detailed results
            is_unused = automator.is_instance_unused(instance_id, volume_ids)
            
            if is_unused:
                print(f"  - Instance is UNUSED (3/4 metrics show low activity)")
                
                # Check DynamoDB for existing state
                try:
                    response = table.get_item(Key={'InstanceId': instance_id})
                    state = response.get('Item')
                except:
                    state = None
                
                current_time = datetime.now().isoformat()
                
                if state:
                    # Check if 15 minutes have passed since warning
                    warning_time = datetime.fromisoformat(state['warning_sent'])
                    if datetime.now() - warning_time >= timedelta(minutes=15):
                        # Time to shutdown
                        print(f"  - 15 minutes elapsed since warning - stopping instance")
                        if automator.stop_instance(instance_id, instance_name):
                            automator.send_shutdown_email(instance_id, instance_name)
                            # Remove from state after shutdown
                            try:
                                table.delete_item(Key={'InstanceId': instance_id})
                            except:
                                pass
                            results['instances_stopped'] += 1
                        else:
                            results['errors'].append(f"Failed to stop {instance_id}")
                    else:
                        time_remaining = 15 - (datetime.now() - warning_time).total_seconds() / 60
                        print(f"  - Waiting for shutdown: {time_remaining:.1f} minutes remaining")
                else:
                    # First time detecting inactivity - send warning and store state
                    state_item = {
                        'InstanceId': instance_id,
                        'instance_name': instance_name,
                        'warning_sent': current_time,
                        'first_detected': current_time,
                        'region': TARGET_REGION,
                        'expiry_time': int((datetime.now() + timedelta(hours=24)).timestamp())  # TTL for 24 hours
                    }
                    
                    try:
                        table.put_item(Item=state_item)
                    except Exception as e:
                        print(f"  - Failed to save state to DynamoDB: {e}")
                    
                    if automator.send_warning_email(instance_id, instance_name):
                        results['warnings_sent'] += 1
                        print(f"  - Warning email sent")
                    else:
                        results['errors'].append(f"Failed to send warning for {instance_id}")
                        
            else:
                print(f"  - Instance is IN USE")
                # Remove from state if it became active again
                try:
                    table.delete_item(Key={'InstanceId': instance_id})
                    print(f"  - Removed from tracking (became active)")
                except:
                    pass
                    
        except Exception as e:
            error_msg = f"Error processing {instance_id}: {str(e)}"
            print(f"  - {error_msg}")
            results['errors'].append(error_msg)
    
    print(f"Processing completed: {results}")
    return {
        'statusCode': 200,
        'body': json.dumps({
            'message': 'EC2 Auto Shutdown completed',
            'results': results,
            'timestamp': datetime.now().isoformat()
        })
    }