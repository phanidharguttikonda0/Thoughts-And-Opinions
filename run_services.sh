#!/bin/bash
DIR="/home/phani/My/Thoughts And Opinions Social Media Application"

cd "$DIR/IdentityService" && nohup ./mvnw spring-boot:run > ../identity.log 2>&1 &
cd "$DIR/ThoughtsService" && nohup ./mvnw spring-boot:run > ../thoughts.log 2>&1 &
cd "$DIR/timelineservice" && nohup ./mvnw spring-boot:run > ../timeline.log 2>&1 &
cd "$DIR/APIGateway" && nohup ./mvnw spring-boot:run > ../apigateway.log 2>&1 &
cd "$DIR/NotificationService" && nohup go run cmd/server/main.go > ../notification.log 2>&1 &
echo "Services started with nohup"
