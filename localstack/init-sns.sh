#!/bin/bash
set -eu

TOPIC_ARN=$(awslocal sns create-topic --name order-persisted --query TopicArn --output text)
QUEUE_URL=$(awslocal sqs create-queue --queue-name order-persisted-inspection --query QueueUrl --output text)
QUEUE_ARN=$(awslocal sqs get-queue-attributes --queue-url "$QUEUE_URL" --attribute-names QueueArn --query Attributes.QueueArn --output text)

awslocal sns subscribe --topic-arn "$TOPIC_ARN" --protocol sqs --notification-endpoint "$QUEUE_ARN" --attributes RawMessageDelivery=true
