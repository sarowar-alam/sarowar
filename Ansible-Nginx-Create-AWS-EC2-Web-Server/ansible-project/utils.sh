#!/bin/bash

source ~/ansible-venv/bin/activate

case "$1" in
    "test-aws")
        ansible localhost -m aws_caller_info
        ;;
    "list-instances")
        ansible-inventory -i inventory/aws_ec2.yml --list
        ;;
    "list-groups")
        ansible-inventory -i inventory/aws_ec2.yml --graph
        ;;
    "ping-all")
        ansible all -m ping
        ;;
    "web-status")
        ansible tag_Role_web -a "systemctl status nginx"
        ;;
    "update-inventory")
        ansible-inventory -i inventory/aws_ec2.yml --refresh-cache
        ;;
    *)
        echo "Usage: $0 {test-aws|list-instances|list-groups|ping-all|web-status|update-inventory}"
        exit 1
        ;;
esac
