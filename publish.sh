mvn clean package -DskipTests

docker build --platform linux/arm64 -t djamn/breakalert:arm64 .
#docker build -t djamn/breakalert:latest .

docker login

echo "Pushing to Docker"
#docker push djamn/breakalert:latest
docker push djamn/breakalert:arm64
