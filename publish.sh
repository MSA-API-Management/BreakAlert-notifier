mvn clean package -DskipTests

docker build -t djamn/ndiff-notifier-v2:latest .

docker login

echo "Pushing to Docker"
docker push djamn/ndiff-notifier-v2:latest