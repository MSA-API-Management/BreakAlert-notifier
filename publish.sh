mvn clean package -DskipTests

docker build -t djamn/ndiff-notifier:latest .

docker login

echo "Pushing to Docker"
docker push djamn/ndiff-notifier:latest