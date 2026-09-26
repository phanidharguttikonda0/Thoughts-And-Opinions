#!/bin/bash
echo "Starting IdentityService..."
(cd IdentityService && ./mvnw clean spring-boot:run > ../identity.log 2>&1) &

echo "Starting ThoughtsService..."
(cd ThoughtsService && ./mvnw clean spring-boot:run > ../thoughts.log 2>&1) &

echo "Starting TimelineService..."
(cd timelineservice && ./mvnw clean spring-boot:run > ../timeline.log 2>&1) &

echo "Starting APIGateway..."
(cd APIGateway && ./mvnw clean spring-boot:run > ../apigateway.log 2>&1) &

echo "Starting NotificationService..."
(cd NotificationService && go run main.go > ../notification.log 2>&1) &

echo "Waiting for services to start..."
for i in {1..20}; do
  if curl -s http://localhost:8080/actuator/health > /dev/null || curl -s http://localhost:8080/ > /dev/null || nc -z localhost 8080; then
    echo "APIGateway seems to be up on 8080."
    break
  fi
  sleep 5
done
echo "Done."
