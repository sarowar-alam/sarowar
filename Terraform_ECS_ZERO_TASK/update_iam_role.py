import boto3
import json
import sys
import botocore.exceptions

def append_arns_to_inline_policy(role_arn, policy_name, arn1, arn2, arn3, profile_name):
    try:
        # Initialize a session using the specified AWS named profile
        session = boto3.Session(profile_name=profile_name)
        iam_client = session.client('iam')  # Use the session instead of boto3.client directly

        # Extract the role name from the role ARN
        role_name = role_arn.split('/')[-1]

        # Retrieve the existing inline policy
        try:
            policy_response = iam_client.get_role_policy(
                RoleName=role_name,
                PolicyName=policy_name
            )
            policy_document = policy_response['PolicyDocument']
        except iam_client.exceptions.NoSuchEntityException:
            print(f"Error: The policy '{policy_name}' or role '{role_name}' does not exist.")
            return
        except botocore.exceptions.ClientError as e:
            print(f"Error retrieving the policy: {e}")
            return

        additional_arns = [arn1, arn2, arn3]

        # Add wildcard ARNs to additional_arns if ARN contains "lambda"
        additional_arns_with_wildcards = []
        for arn in additional_arns:
            print (arn)
            additional_arns_with_wildcards.append(arn)  # Add the original ARN
            if "lambda" in arn:
                arn_with_wildcard = f"{arn}:*"
                print (arn_with_wildcard)
                additional_arns_with_wildcards.append(arn_with_wildcard)  # Add wildcard ARN

        # Now use the updated list with wildcard ARNs in your policy update
        policy_updated = False
        for statement in policy_document.get('Statement', []):
            if statement.get('Sid') == 'UpdateHereOnly' and statement['Effect'] == 'Allow' and isinstance(statement['Resource'], list):
                for arn in additional_arns_with_wildcards:
                    if arn not in statement['Resource']:
                        statement['Resource'].append(arn)
                        policy_updated = True


        if not policy_updated:
            print("No changes were made to the policy (ARNs may already exist).")
            return

        # Update the inline policy with the modified policy document
        try:
            iam_client.put_role_policy(
                RoleName=role_name,
                PolicyName=policy_name,
                PolicyDocument=json.dumps(policy_document)
            )
            print("Policy updated successfully.")
        except botocore.exceptions.ClientError as e:
            print(f"Error updating the policy: {e}")
        except json.JSONDecodeError:
            print("Error: Failed to encode the policy document as JSON.")
    except botocore.exceptions.ProfileNotFound:
        print(f"Error: AWS profile '{profile_name}' not found. Check your AWS credentials.")
    except Exception as e:
        print(f"Unexpected error: {e}")

if __name__ == "__main__":
    if len(sys.argv) < 5:
        print("Usage: python script.py <role_arn> <policy_name> <arn1> <arn2>")
        sys.exit(1)

    profile_name = 'ecs-zero-task-update-iam-role-prod'  # Replace with your AWS named profile
    append_arns_to_inline_policy(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], profile_name)
